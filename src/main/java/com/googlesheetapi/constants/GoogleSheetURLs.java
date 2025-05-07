package com.googlesheetapi.constants;

public class GoogleSheetURLs {
	public static final String OAUTH_ACCESS_TOKEN_URL = "https://accounts.google.com/o/oauth2/token";
	public static final String GOOGLE_DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files?supportsAllDrives=false&corpora=user&fields=nextPageToken%2C%20files%28id%2Cname%2CdriveId%2CmimeType%20%29&includeItemsFromAllDrives=false&q=trashed%3Dfalse%20AND%20mimeType%3D%27application%2Fvnd.google-apps.spreadsheet%27%20AND%20%28name%3D%27Testing%27%29&pageSize=1000";
	public static final String SPREADSHEET_INFO_URL = "https://sheets.googleapis.com/v4/spreadsheets/1KUsw5wFZA8jrvChziBe5YJ2Ggj5eja0weE5TjEB7LFU?includeGridData=false";
	public static final String SHEET_COLUMNS_GET_URL = "https://sheets.googleapis.com/v4/spreadsheets/%s/values/'%s'!A1:Z10";
	public static final String SHEET_ALL_DATA_GET_URL = "https://sheets.googleapis.com/v4/spreadsheets/%s/values/'%s'!A1:ZZZ";
	public static final String SPREADSHEET_EXPORT_URL = "https://sheets.googleapis.com/v4/spreadsheets/%s/values/%s:append?valueInputOption=USER_ENTERED";

	private GoogleSheetURLs() {
	}
}
