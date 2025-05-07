package com.googlesheetapi.domain.entity;

public class OauthCredentials {
	String clientId;
    String clientSecret;
    String refreshToken;
    final String grantType = "refresh_token";

    public OauthCredentials(String clientId, String clientSecret, String refreshToken) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
    }
    
    public String getClientId() {
  		return clientId;
  	}

  	public void setClientId(String clientId) {
  		this.clientId = clientId;
  	}

  	public String getClientSecret() {
  		return clientSecret;
  	}

  	public void setClientSecret(String clientSecret) {
  		this.clientSecret = clientSecret;
  	}

  	public String getRefreshToken() {
  		return refreshToken;
  	}

  	public void setRefreshToken(String refreshToken) {
  		this.refreshToken = refreshToken;
  	}

  	public String getGrantType() {
  		return grantType;
  	}
}
