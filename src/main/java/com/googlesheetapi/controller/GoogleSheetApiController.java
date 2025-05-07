package com.googlesheetapi.controller;

import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.googlesheetapi.constants.AppConstants;
import com.googlesheetapi.domain.GoogleSheetApiService;
import com.googlesheetapi.domain.entity.OauthCredentials;
import com.googlesheetapi.exceptions.BusinessLogicException;

@RestController
public class GoogleSheetApiController {

	OauthCredentials credentials = new OauthCredentials(AppConstants.CLIENT_ID, AppConstants.CLIENT_SECRET,
			AppConstants.REFRESH_TOKEN);

	@Autowired
	GoogleSheetApiService gsApiService;

	@GetMapping("/tableNames")
	public List<String> getAlltableNames() throws BusinessLogicException {
		List<String> spreadSheet = gsApiService.getTableNames(credentials);
		return spreadSheet;
	}

	@GetMapping("/columnNames")
	public List<String> getAllColumnNames(@RequestParam String tableName) throws BusinessLogicException {
		List<String> spreadSheetColumn = gsApiService.getColumnNames(credentials, tableName);
		return spreadSheetColumn;
	}

	@GetMapping("/columnNamesAndValues")
	public List<Map<String, String>> getAllColumnNamesAndValues(@RequestParam String tableName)
			throws BusinessLogicException {
		List<Map<String, String>> spreadSheetColumnAndValues = gsApiService.getColumnNamesAndValues(credentials,
				tableName);
		return spreadSheetColumnAndValues;
	}

	@PostMapping("/insertRow")
	public ResponseEntity<String> insertRow(@RequestParam String sheetName, @RequestParam String shopName,
			@RequestParam String address, @RequestParam String shopType) throws BusinessLogicException {

		List<Object> rowData = List.of(shopName, address, shopType);

		gsApiService.insertRowToSheet(credentials, sheetName, rowData);
		return ResponseEntity.ok("Row inserted successfully");
	}

	@GetMapping("/outputCsvForAllData")
	public ResponseEntity<String> outputCsvForAllData(@RequestParam String tableName,
			@RequestParam(defaultValue = "false") Boolean isTSV) throws BusinessLogicException, ParseException {
		gsApiService.exportTableToCsv(credentials, tableName, isTSV);
		return ResponseEntity.ok("Row inserted successfully");
	}
}
