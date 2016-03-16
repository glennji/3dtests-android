package com.glennji;

import android.app.Activity;
import android.os.Bundle;

public class ZomNomNomView extends Activity {

	/** Our main game engine */
	private ZomEngine engine;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		engine = new ZomEngine(this.getApplication());
		setContentView(engine.getContentView());
	}

}