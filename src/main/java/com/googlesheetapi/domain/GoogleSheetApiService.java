package com.googlesheetapi.domain;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.googlesheetapi.constants.AppConstants;
import com.googlesheetapi.constants.GoogleSheetURLs;
import com.googlesheetapi.domain.entity.FilesResponse;
import com.googlesheetapi.domain.entity.OauthCredentials;
import com.googlesheetapi.exceptions.BusinessLogicException;
import com.opencsv.CSVWriter;

@Service
public class GoogleSheetApiService {

	private static final String OAUTH_CREDENTIALS_FORMAT = "{ \"client_id\": \"%s\", \"client_secret\": \"%s\", \"refresh_token\": \"%s\", \"grant_type\": \"%s\" }";

	public List<String> getTableNames(OauthCredentials credentials) throws BusinessLogicException {
		String accessToken = this.getAccessToken(credentials);
		List<String> spreadsheetIds = getSpreadsheetIds(accessToken);
		return getTableNamesFromSpreadsheetIds(spreadsheetIds, accessToken);
	}

	public List<String> getColumnNames(OauthCredentials credentials, String tableName) throws BusinessLogicException {
		List<String> columnNames = new ArrayList<>();

		String sheetDataGetUrl = getFullURL(credentials, tableName, GoogleSheetURLs.SHEET_COLUMNS_GET_URL);
		String accessToken = this.getAccessToken(credentials);

		final HttpGet sheetDataGetRequest = new HttpGet(sheetDataGetUrl);
		sheetDataGetRequest.setHeader("Authorization", "Bearer " + accessToken);

		try (CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse response = httpClient.execute(sheetDataGetRequest);) {
			String responseString = EntityUtils.toString(response.getEntity());
			JsonObject responseJson = JsonParser.parseString(responseString).getAsJsonObject();
			Optional<JsonArray> optionalJsonArray = Optional.ofNullable(responseJson.getAsJsonArray("values"));

			if (optionalJsonArray.isPresent()) {
				Gson gson = new Gson();
				Type type = new TypeToken<List<String>>() {
				}.getType();
				columnNames = gson.fromJson(optionalJsonArray.get().get(0), type);
			}
		} catch (Exception e) {
			throw new BusinessLogicException("Failed to sheet data", e);
		}
		return columnNames;
	}

	public List<Map<String, String>> getColumnNamesAndValues(OauthCredentials credentials, String tableName)
			throws BusinessLogicException {

		List<Map<String, String>> resultList = new ArrayList<>();
		String accessToken = this.getAccessToken(credentials);
		String sheetDataGetUrl = getFullURL(credentials, tableName, GoogleSheetURLs.SHEET_COLUMNS_GET_URL);

		final HttpGet sheetDataGetRequest = new HttpGet(sheetDataGetUrl);
		sheetDataGetRequest.setHeader("Authorization", "Bearer " + accessToken);

		try (CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse response = httpClient.execute(sheetDataGetRequest);) {
			String responseString = EntityUtils.toString(response.getEntity());
			JsonObject responseJson = JsonParser.parseString(responseString).getAsJsonObject();
			JsonArray values = responseJson.getAsJsonArray("values");

			if (values.size() >= 2) {
				JsonArray headers = values.get(0).getAsJsonArray();

				for (int i = 1; i < values.size(); i++) {
					JsonArray row = values.get(i).getAsJsonArray();
					Map<String, String> rowMap = new LinkedHashMap<>();

					for (int j = 0; j < headers.size() && j < row.size(); j++) {
						String key = headers.get(j).getAsString();
						String value = row.get(j).getAsString();
						rowMap.put(key, value);
					}
					resultList.add(rowMap);
				}
			}

		} catch (Exception e) {
			throw new BusinessLogicException("Failed to sheet data", e);
		}
		return resultList;
	}

	public List<String> getSpreadsheetIds(String accessToken) throws BusinessLogicException {
		List<String> spreadsheetIds = new ArrayList<>();

		final HttpGet request = new HttpGet(GoogleSheetURLs.GOOGLE_DRIVE_FILES_URL);
		request.setHeader("Authorization", "Bearer " + accessToken);

		try (CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse response = httpClient.execute(request);) {
			String responseString = EntityUtils.toString(response.getEntity());
			Gson gson = new Gson();
			FilesResponse filesResponse = gson.fromJson(responseString, FilesResponse.class);
			filesResponse.getFiles().forEach(file -> {
				spreadsheetIds.add(file.getId());
			});
		} catch (Exception e) {
			throw new BusinessLogicException("Failed to get spreadsheet IDs", e);
		}

		return spreadsheetIds;
	}

	private List<String> getTableNamesFromSpreadsheetIds(List<String> spreadsheetIds, String accessToken)
			throws BusinessLogicException {
		List<String> result = new ArrayList<>();

		for (String spreadsheetId : spreadsheetIds) {
			List<String> tableNames = getTableNamesBySheetId(spreadsheetId, accessToken);
			result.addAll(tableNames);
		}

		return result;
	}

	private List<String> getTableNamesBySheetId(String spreadsheetId, String accessToken)
			throws BusinessLogicException {
		List<String> tableNames = new ArrayList<>();
		String newUrl = String.format(GoogleSheetURLs.SPREADSHEET_INFO_URL, spreadsheetId);

		int responseCode;
		int retryCount = 0;
		do {
			final HttpGet request = new HttpGet(newUrl);
			request.setHeader("Authorization", "Bearer " + accessToken);

			try (CloseableHttpClient httpClient = HttpClients.createDefault();
					CloseableHttpResponse response = httpClient.execute(request)) {

				responseCode = response.getCode();

				switch (responseCode) {
				case HttpStatus.SC_OK:
					List<String> tables = getResponseData(response);
					tableNames.addAll(tables);
					return tableNames;
				case HttpStatus.SC_TOO_MANY_REQUESTS:
					retryCount++;
					System.out.println("Retry attempt => " + retryCount);
					Thread.sleep(1000 * retryCount);
					break;
				default:
					throw new BusinessLogicException(
							"Failed to get table names by sheet ID, response code: " + responseCode, responseCode);
				}
			} catch (Exception e) {
				throw new BusinessLogicException("Failed to get table names by sheet ID", e);
			}
		} while (responseCode == HttpStatus.SC_TOO_MANY_REQUESTS && retryCount < AppConstants.MAX_RETRY_COUNT);

		if (responseCode == HttpStatus.SC_TOO_MANY_REQUESTS) {
			throw new BusinessLogicException(
					"Failed to get google spreadsheet after " + AppConstants.MAX_RETRY_COUNT + " retries",
					responseCode);
		}
		return tableNames;
	}

	private List<String> getResponseData(CloseableHttpResponse response) throws IOException, ParseException {
		List<String> tableNames = new ArrayList<>();
		String responseString = EntityUtils.toString(response.getEntity());
		JsonObject jsonObject = JsonParser.parseString(responseString).getAsJsonObject();

		String spreadsheetName = jsonObject.getAsJsonObject("properties").get("title").getAsString();

		JsonArray sheets = jsonObject.getAsJsonArray("sheets");
		sheets.forEach(sheet -> {
			tableNames.add(spreadsheetName + "_"
					+ sheet.getAsJsonObject().getAsJsonObject("properties").get("title").getAsString());
		});
		return tableNames;
	}

	public String getAccessToken(OauthCredentials credentials) throws BusinessLogicException {

		final HttpPost request = new HttpPost(GoogleSheetURLs.OAUTH_ACCESS_TOKEN_URL);

		String json = String.format(OAUTH_CREDENTIALS_FORMAT, credentials.getClientId(), credentials.getClientSecret(),
				credentials.getRefreshToken(), credentials.getGrantType());

		StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
		request.setEntity(entity);

		try (CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse response = httpClient.execute(request);) {
			String responseString = EntityUtils.toString(response.getEntity());
			JsonObject responseJson = JsonParser.parseString(responseString).getAsJsonObject();

			return responseJson.get("access_token").getAsString();
		} catch (Exception e) {
			throw new BusinessLogicException("Failed to get access token", e);
		}
	}

	private Map<String, String> getFilteredSpreadsheetData(String tableName, List<JsonObject> spreadsheetFiles,
			String accessToken) {
		Map<String, String> result = new HashMap<>();
		outerloop: for (JsonObject file : spreadsheetFiles) {

			List<String> tableNames = getTableNamesBySheetId(file.get("id").getAsString(), accessToken);
			try {
				for (String name : tableNames) {
					if (tableName.equals(name)) {
						result.put("spreadsheetId", file.get("id").getAsString());
						result.put("sheet", tableName.replaceFirst(file.get("name").getAsString() + "_", ""));
						break outerloop;
					}
				}
			} catch (BusinessLogicException e) {
				throw new BusinessLogicException("Failed to sheet data", e);
			}
		}
		return result;
	}

	public void insertRowToSheet(OauthCredentials credentials, String tableName, List<Object> rowData)
			throws BusinessLogicException {

		String sheetDataGetUrl = getFullURL(credentials, tableName, GoogleSheetURLs.SPREADSHEET_EXPORT_URL);
		String accessToken = this.getAccessToken(credentials);

		JsonObject body = new JsonObject();
		JsonArray values = new JsonArray();
		JsonArray row = new JsonArray();

		for (Object val : rowData) {
			row.add(val.toString());
		}

		values.add(row);
		body.add("values", values);

		final HttpPost post = new HttpPost(sheetDataGetUrl);
		post.setHeader("Authorization", "Bearer " + accessToken);
		post.setHeader("Content-Type", "application/json");
		post.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON));

		try (CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse response = httpClient.execute(post)) {

			int code = response.getCode();
			if (code != 200 && code != 201) {
				throw new BusinessLogicException("Failed to insert row, HTTP code: ", code);
			}

		} catch (IOException e) {
			throw new BusinessLogicException("Error occurred while inserting row", e);
		}
	}

	public String getFullURL(OauthCredentials credentials, String tableName, String googleSheetURL) {
		String accessToken = this.getAccessToken(credentials);
		List<JsonObject> tempData = new ArrayList<>();

		Map<String, String> data = new HashMap<>();

		final HttpGet request = new HttpGet(GoogleSheetURLs.GOOGLE_DRIVE_FILES_URL);
		request.setHeader("Authorization", "Bearer " + accessToken);

		try (CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse response = httpClient.execute(request);) {
			String responseString = EntityUtils.toString(response.getEntity());
			JsonObject responseJson = JsonParser.parseString(responseString).getAsJsonObject();

			responseJson.getAsJsonArray("files").forEach(file -> {
				String fileName = file.getAsJsonObject().get("name").getAsString();
				if (tableName.startsWith(fileName)) {
					tempData.add(file.getAsJsonObject());
				}
			});
		} catch (Exception e) {
			throw new BusinessLogicException("Failed to get spreadsheet IDs", e);
		}

		if (tempData.size() > 0) {
			data = getFilteredSpreadsheetData(tableName, tempData, accessToken);
		}

		String sheetDataGetUrl = String.format(googleSheetURL, data.get("spreadsheetId"), data.get("sheet"));

		return sheetDataGetUrl;
	}

	public void exportTableToCsv(OauthCredentials credentials, String tableName, boolean isTSV)
			throws BusinessLogicException, ParseException {
		String accessToken = this.getAccessToken(credentials);
		List<JsonObject> spreadsheetFiles = fetchMatchingSpreadsheetFiles(accessToken, tableName);

		if (spreadsheetFiles.isEmpty()) {
			throw new BusinessLogicException("No spreadsheets found for table: " + tableName);
		}

		Map<String, String> spreadsheetInfo = extractSpreadsheetInfo(tableName, spreadsheetFiles);
		writeCsvFromSpreadsheet(spreadsheetInfo, accessToken, isTSV, tableName);
	}

	private List<JsonObject> fetchMatchingSpreadsheetFiles(String accessToken, String tablePrefix)
			throws BusinessLogicException, ParseException {
		List<JsonObject> spreadsheetFiles = new ArrayList<>();
		final HttpGet request = new HttpGet(GoogleSheetURLs.GOOGLE_DRIVE_FILES_URL);
		request.setHeader("Authorization", "Bearer " + accessToken);

		try (CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse response = httpClient.execute(request);) {
			String responseString = EntityUtils.toString(response.getEntity());
			JsonObject responseJson = JsonParser.parseString(responseString).getAsJsonObject();

			if (response.getCode() == HttpStatus.SC_OK) {
				Optional<JsonArray> files = Optional.ofNullable(responseJson.getAsJsonArray("files"));
				files.ifPresent(jsonFiles -> jsonFiles.forEach(file -> {
					Optional<JsonElement> name = Optional.ofNullable(file.getAsJsonObject().get("name"));
					name.map(JsonElement::getAsString).filter(tablePrefix::startsWith)
							.ifPresent(fileName -> spreadsheetFiles.add(file.getAsJsonObject()));
				}));
			} else {
				String errorMsg = responseJson.getAsJsonObject("error").get("message").getAsString();
				throw new BusinessLogicException(errorMsg, response.getCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessLogicException("An error occurs. ", e);
		}
		return spreadsheetFiles;
	}

	private Map<String, String> extractSpreadsheetInfo(String tableName, List<JsonObject> spreadsheetFiles) {
		JsonObject spreadsheet = spreadsheetFiles.get(0);
		String fileId = spreadsheet.get("id").getAsString();
		String sheetName = tableName.replaceFirst(spreadsheet.get("name").getAsString() + "_", "");

		Map<String, String> info = new HashMap<>();
		info.put("spreadsheetId", fileId);
		info.put("sheet", sheetName);
		return info;
	}

	// === CSV Writer ===

	private void writeCsvFromSpreadsheet(Map<String, String> spreadsheetInfo, String accessToken, boolean isTSV,
			String tableName) throws BusinessLogicException, ParseException {
		String url = String.format(GoogleSheetURLs.SHEET_ALL_DATA_GET_URL, spreadsheetInfo.get("spreadsheetId"),
				spreadsheetInfo.get("sheet"));
		HttpGet request = new HttpGet(url);
		request.setHeader("Authorization", "Bearer " + accessToken);

		try (CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse response = httpClient.execute(request)) {

			String responseString = EntityUtils.toString(response.getEntity());
			JsonObject json = JsonParser.parseString(responseString).getAsJsonObject();
			JsonArray values = json.getAsJsonArray("values");

			if (values == null) {
				throw new BusinessLogicException("No data returned for sheet.");
			}

			writeCsvFile(values, isTSV, tableName);

		} catch (IOException e) {
			throw new BusinessLogicException("Failed to read sheet data", e);
		}
	}

	private void writeCsvFile(JsonArray rows, boolean isTSV, String tableName) throws IOException {
		Gson gson = new Gson();
		Type listType = new TypeToken<List<String>>() {
		}.getType();
		String outputPath = generateUniqueCsvPath(tableName, isTSV);

		try (Writer writer = new FileWriter(outputPath);
				CSVWriter csvWriter = new CSVWriter(writer, isTSV ? '\t' : CSVWriter.DEFAULT_SEPARATOR,
						CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END)) {

			for (JsonElement rowElement : rows) {
				JsonArray rowArray = rowElement.getAsJsonArray();
				List<String> row = gson.fromJson(rowArray, listType);
				csvWriter.writeNext(row.toArray(new String[0]));
			}
		}
	}

	private String generateUniqueCsvPath(String tableName, boolean isTSV) {
		final String baseDir = "D:\\DataX\\csvs\\testing\\";
		final String extension = isTSV ? ".tsv" : ".csv";
		int index = 0;
		String filePath;

		do {
			String suffix = (index == 0) ? "" : "(" + index + ")";
			filePath = baseDir + tableName + suffix + extension;
			index++;
		} while (Files.exists(Paths.get(filePath)));

		return filePath;
	}
}
