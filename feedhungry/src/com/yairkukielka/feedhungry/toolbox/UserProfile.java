package com.yairkukielka.feedhungry.toolbox;

public class UserProfile {
    private String accessToken;
    private String refreshToken;
    private String plan;
    private String tokenType;
    private String expires;
    
	public UserProfile(String accessToken, String refreshToken, String plan,
			String tokenType, String expires) {
		super();
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.plan = plan;
		this.tokenType = tokenType;
		this.expires = expires;
	}
	
	public String getAccessToken() {
		return accessToken;
	}
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	public String getRefreshToken() {
		return refreshToken;
	}
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
	public String getPlan() {
		return plan;
	}
	public void setPlan(String plan) {
		this.plan = plan;
	}
	public String getTokenType() {
		return tokenType;
	}
	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}
	public String getExpires() {
		return expires;
	}
	public void setExpires(String expires) {
		this.expires = expires;
	}
    
    
}
