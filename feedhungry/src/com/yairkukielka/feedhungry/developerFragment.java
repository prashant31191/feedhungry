package com.yairkukielka.feedhungry;

import android.annotation.SuppressLint;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.SherlockFragment;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;


@EFragment(R.layout.developer_layout)
public class developerFragment extends SherlockFragment {
	
	// animation to show the feed content after the loading fragment shows
	private Animation webViewAnimation;
	// fragment that shows while loading the entry content
	private Fragment loadingFragment;
	@ViewById(R.id.developer_webview)
	WebView webView;
	
	@SuppressLint("SetJavaScriptEnabled")
	@AfterViews
	void afterViews() {
		webView.setVisibility(View.INVISIBLE);
		webView.getSettings().setJavaScriptEnabled(true);
		// animate webview content
		webViewAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.push_up_in);
		loadingFragment = new LoadingFragment_();
		getFragmentManager().beginTransaction().replace(R.id.frame_webview, loadingFragment).commit();
		webView.loadUrl("http://www.about.me/yair.kukielka");
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				FragmentManager fragmentManager = getFragmentManager();
				if (null != fragmentManager.findFragmentById(loadingFragment.getId())) {
					fragmentManager.beginTransaction().remove(loadingFragment).commit();
				}
				webView.setAnimation(webViewAnimation);
				webView.setVisibility(View.VISIBLE);
			}
		});
	}
	
	
}