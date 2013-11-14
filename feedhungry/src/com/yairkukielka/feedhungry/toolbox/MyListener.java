package com.yairkukielka.feedhungry.toolbox;

public abstract class MyListener {

	/** Callback interface for delivering parsed responses. */
	public interface TokenListener {
		/** Called when a response is received. */
		public void onResponse(String accessToken);
	}

	/** Callback interface for delivering error responses. */
	public interface ErrorTokenListener {
		/**
		 * Callback method that an error has been occurred with the provided
		 * error code and optional user-readable message.
		 */
		public void onErrorResponse(String error);
	}
}
