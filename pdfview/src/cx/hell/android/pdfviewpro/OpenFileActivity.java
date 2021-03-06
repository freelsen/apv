package cx.hell.android.pdfviewpro;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import cx.hell.android.lib.pagesview.FindResult;
import cx.hell.android.lib.pagesview.PagesView;
import cx.hell.android.lib.pdf.PDF;

// #ifdef pro
import java.util.Map;
import android.content.DialogInterface.OnDismissListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ScrollView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import cx.hell.android.lib.pdf.PDF.Outline;
import cx.hell.android.lib.view.TreeView;
// #endif


/**
 * Document display activity.
 */
public class OpenFileActivity extends Activity implements SensorEventListener {

	private final static String TAG = "cx.hell.android.pdfviewpro";
	
	private final static int[] zoomAnimations = {
		R.anim.zoom_disappear, R.anim.zoom_almost_disappear, R.anim.zoom
	};
	
	private final static int[] pageNumberAnimations = {
		R.anim.page_disappear, R.anim.page_almost_disappear, R.anim.page, 
		R.anim.page_show_always
	};
	
	private PDF pdf = null;
	private PagesView pagesView = null;
	
// #ifdef pro
	
	/**
	 * Complete top-level view (layout) of text reflow.
	 * Hidden (with Visibility.GONE) when not in text reflow mode.
	 */
	private View textReflowView = null;
	
	/**
	 * View that contains scrollable view(s) visible in text reflow mode.
	 */
	private ScrollView textReflowScrollView = null;
	
	/**
	 * TextView visible in text reflow mode, contains text extracted from PDF file.
	 */
	private TextView textReflowTextView = null;

// #endif

	private PDFPagesProvider pdfPagesProvider = null;
	private Actions actions = null;
	
	private Handler zoomHandler = null;
	private Handler pageHandler = null;
	private Runnable zoomRunnable = null;
	private Runnable pageRunnable = null;
	
	private MenuItem aboutMenuItem = null;
	private MenuItem gotoPageMenuItem = null;
	private MenuItem rotateLeftMenuItem = null;
	private MenuItem rotateRightMenuItem = null;
	private MenuItem findTextMenuItem = null;
	private MenuItem clearFindTextMenuItem = null;
	private MenuItem chooseFileMenuItem = null;
	private MenuItem optionsMenuItem = null;
	// #ifdef pro
	private MenuItem tableOfContentsMenuItem = null;
	private MenuItem textReflowMenuItem = null;
	// #endif
	
	private EditText pageNumberInputField = null;
	private EditText findTextInputField = null;
	
	private LinearLayout findButtonsLayout = null;
	private Button findPrevButton = null;
	private Button findNextButton = null;
	private Button findHideButton = null;
	
	private RelativeLayout activityLayout = null;
	private boolean eink = false;	

	// currently opened file path
	private String filePath = "/";
	
	private String findText = null;
	private Integer currentFindResultPage = null;
	private Integer currentFindResultNumber = null;

	// zoom buttons, layout and fade animation
/*	
	private ImageButton zoomDownButton;
	private ImageButton zoomWidthButton;
	private ImageButton zoomUpButton;
*/	
	// bottom toolbar;	
	private Button zoomDownButton;
	private Button zoomUpButton;
	
//=== +LS; 2013-02-04; ===	//private ImageButton btn_zoom_n1;
	public boolean isshowzoom = false;
	// 4 top buttons;
	private Button btn_zoom_t;		// Menu;	
	private Button btn_zoom_rbar;	// read bar( pages);
	private Button btn_zoom_zbar;// zoom bar;
	private Button zoomWidthButton;// horizen move bar;

	private Animation zoomAnim;
	private LinearLayout zoomLayout;
	// 2 rows rmb buttons;
	int rzoom_show = 1;				// 0: hide; 1: read bar; 2: zoom bar; 3= move bar;
	private Button btn_zoom_p1;
	private Button btn_zoom_p2;
	private Button btn_zoom_p3;
	private Button btn_zoom_p4;
	private Button btn_zoom_p5;
	private Button btn_zoom_p6;
	private Button btn_zoom_n1;
	private Button btn_zoom_n2;
	private Button btn_zoom_n3;
	private Button btn_zoom_n4;
	private Button btn_zoom_n5;
	private Button btn_zoom_n6;
	private Button btn_zoom_f;
	private Button btn_zoom_e;
// +ls; 2013-02-06;
	private LinearLayout zoomLayout2;	// 2013-02-06; AM;
	private LinearLayout zoomLayout3;
// -ls;
	
	// page number display
	private TextView pageNumberTextView;
	private Animation pageNumberAnim;
	
	private int box = 2;

	public boolean showZoomOnScroll = false;
	
	private int fadeStartOffset = 7000; 
	
	private int colorMode = Options.COLOR_MODE_NORMAL;

	private SensorManager sensorManager;
	private static final int ZOOM_COLOR_NORMAL = 0;
	private static final int ZOOM_COLOR_RED = 1;
	private static final int ZOOM_COLOR_GREEN = 2;
	private static final int[] zoomUpId = {
		R.drawable.btn_zoom_up, R.drawable.red_btn_zoom_up, R.drawable.green_btn_zoom_up
	};
	private static final int[] zoomDownId = {
		R.drawable.btn_zoom_down, R.drawable.red_btn_zoom_down, R.drawable.green_btn_zoom_down		
	};
	private static final int[] zoomWidthId = {
		R.drawable.btn_zoom_width, R.drawable.red_btn_zoom_width, R.drawable.green_btn_zoom_width		
	};
	private float[] gravity = { 0f, -9.81f, 0f};
	private long gravityAge = 0;

	private int prevOrientation;

	private boolean history = true;
	
// #ifdef pro
	/**
	 * If true, then current activity is in text reflow mode.
	 */
	private boolean textReflowMode = false;
// #endif
	
	// +ls: 140302;
	private LsMenu mlsmenu = new LsMenu();
	
	public LsMenu getLsmenu() {
		return mlsmenu;
	}

	/**
     * Called when the activity is first created.
     * TODO: initialize dialog fast, then move file loading to other thread
     * TODO: add progress bar for file load
     * TODO: add progress icon for file rendering
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate(" + savedInstanceState + ")");
        
		Options.setOrientation(this);
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);

		this.box = Integer.parseInt(options.getString(Options.PREF_BOX, "2"));
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Get display metrics
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        // use a relative layout to stack the views
        activityLayout = new RelativeLayout(this);
        
        // the PDF view
        this.pagesView = new PagesView(this);
        activityLayout.addView(pagesView);
        startPDF(options);
        if (!this.pdf.isValid()) {
        	finish();
        }
        
// #ifdef pro
        /* TODO: move to separate method */
        LinearLayout textReflowLayout = new LinearLayout(this);
        this.textReflowView = textReflowLayout;
        textReflowLayout.setOrientation(LinearLayout.VERTICAL);
        
        this.textReflowScrollView = new ScrollView(this);
        this.textReflowScrollView.setFillViewport(true);
        
        this.textReflowTextView = new TextView(this);
        
        LinearLayout textReflowButtonsLayout = new LinearLayout(this);
        textReflowButtonsLayout.setGravity(Gravity.CENTER);
        textReflowButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        Button textReflowPrevPageButton = new Button(this);
        textReflowPrevPageButton.setText("Prev");
        textReflowPrevPageButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				OpenFileActivity.this.nextPage(-1);
			}
        });
        Button textReflowNextPageButton = new Button(this);
        textReflowNextPageButton.setText("Next");
        textReflowNextPageButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		OpenFileActivity.this.nextPage(1);
        	}
        });
        textReflowButtonsLayout.addView(textReflowPrevPageButton);
        textReflowButtonsLayout.addView(textReflowNextPageButton);

        this.textReflowScrollView.addView(this.textReflowTextView);
        LinearLayout.LayoutParams textReflowScrollViewLayoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
        textReflowLayout.addView(this.textReflowScrollView, textReflowScrollViewLayoutParams);
        textReflowLayout.addView(textReflowButtonsLayout, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0));

        activityLayout.addView(this.textReflowView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT));
        this.textReflowView.setVisibility(View.GONE);
        AndroidReflections.setScrollbarFadingEnabled(this.textReflowView, true);
// #endif
        
        // the find buttons
        this.findButtonsLayout = new LinearLayout(this);
        this.findButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        this.findButtonsLayout.setVisibility(View.GONE);
        this.findButtonsLayout.setGravity(Gravity.CENTER);
        this.findPrevButton = new Button(this);
        this.findPrevButton.setText("Prev");
        this.findButtonsLayout.addView(this.findPrevButton);
        this.findNextButton = new Button(this);
        this.findNextButton.setText("Next");
        this.findButtonsLayout.addView(this.findNextButton);
        this.findHideButton = new Button(this);
        this.findHideButton.setText("Hide");
        this.findButtonsLayout.addView(this.findHideButton);
        this.setFindButtonHandlers();
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
        		RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        activityLayout.addView(this.findButtonsLayout, lp);

        this.pageNumberTextView = new TextView(this);
        this.pageNumberTextView.setTextSize(8f*metrics.density);
        lp = new RelativeLayout.LayoutParams(
        		RelativeLayout.LayoutParams.WRAP_CONTENT, 
        		RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        activityLayout.addView(this.pageNumberTextView, lp);
        
		// display this
        this.setContentView(activityLayout);
        
        // go to last viewed page
//        gotoLastPage();
        
        // send keyboard events to this view
        pagesView.setFocusable(true);
        pagesView.setFocusableInTouchMode(true);

        this.zoomHandler = new Handler();
        this.pageHandler = new Handler();
        this.zoomRunnable = new Runnable() {
        	public void run() {
        		fadeZoom();
        	}
        };
        this.pageRunnable = new Runnable() {
        	public void run() {
        		fadePage();
        	}
        };
    }

	/** 
	 * Save the current page before exiting
	 */
	@Override
	protected void onPause() {
		super.onPause();

		saveLastPage();
		
		ls_setOrientOnPause(); //@2013-07-24,8:41;		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);

		ls_setOrient(options); //@2013-07-24;7:55;
		ls_setHistory(options); //@2013-07-24,8:23;
		ls_setEink(options); //@2013-07-24;7:59;
		ls_setKeepScreen(options); //@2013-07-24;8:01;
		ls_setActions(options); //@2013-07-24,8:25;

		setZoomLayout(options);
		
		ls_setZoomLayout(); // @2013-07-23.17:35;
		ls_setShowZoomScroll(options); //@2013-07-24,8:06;
		ls_setMargin(options); // @2013-07-24,8:06;
		ls_setDoubleTap(options); //@2013-07-24,8:08;
		ls_setBox(options); //@2013-07-24,8:10;
        //this.eink; -> ls_setEink(); //@2013-07-24,8:20;
		ls_setColor(options); //@2013-07-24,8:18;
        ls_setCache(options); //@2013-07-24,8:15;
        ls_setImages(options); //@2013-07-24,8:17;
		ls_setRender(options); //@2013-07-24,8:15;
		ls_setVerticalLock(options); //@2013-07-24,8:16;		

		this.pagesView.ls_invalidate();
				
		ls_setZoomAnim(options); //@2013-07-23;23:24; +ls;
		ls_setPageNumAnim(options); //@2013-07-23;23:24; +ls;
		ls_setFade(options); //@2013-07-24,8:32;
		ls_setFullScreen(options); //@2013-07-24,8:27;
		ls_showPageNumText(); //@2013-07-23;23:28; +ls;

// #ifdef pro
		if( !ls_isZoomAnim() || this.textReflowMode ) // @2013-07-23.17:53;
			ls_showZoomLayout( false );
		else
			ls_showZoomLayout( true );
		//	ls_showZoomLayout_onresume(); //@2013-07-23.17:40;
// #endif
// #ifdef lite
		if( ls_isZoomAnim())//@2013-07-24,8:35;
			ls_showZoomLayout(true);
		else
			ls_showZoomLayout(false);
// #endif
        
        showAnimated(true);
	}
	
	public void onStop() {
	    super.onStop();
	    Log.i(TAG, "onStop()");
	}
	
	public void onDestroy() {
	    super.onDestroy();
	    Log.i(TAG, "onDestroy()");
	    this.pdf.freeMemory(); /* gc is too slow, code must make sure double free is not possible */
	}
   
    private void startPDF(SharedPreferences options) {
	    this.pdf = this.getPDF();
	    if (!this.pdf.isValid()) {
	    	Log.v(TAG, "Invalid PDF");
	    	if (this.pdf.isInvalidPassword()) {
	    		Toast.makeText(this, "This file needs a password", Toast.LENGTH_SHORT).show();
	    	}
	    	else {
	    		Toast.makeText(this, "Invalid PDF file", Toast.LENGTH_SHORT).show();
	    	}
	    	return;
	    }
	    this.colorMode = Options.getColorMode(options);
	    this.pdfPagesProvider = new PDFPagesProvider(this, pdf, 
	    		options.getBoolean(Options.PREF_OMIT_IMAGES, false),
	    		options.getBoolean(Options.PREF_RENDER_AHEAD, true));
	    pagesView.setPagesProvider(pdfPagesProvider);
	    Bookmark b = new Bookmark(this.getApplicationContext()).open();
	    pagesView.setStartBookmark(b, filePath);
	    b.close();
    }

    /**
     * Return PDF instance wrapping file referenced by Intent.
     * Currently reads all bytes to memory, in future local files
     * should be passed to native code and remote ones should
     * be downloaded to local tmp dir.
     * @return PDF instance
     */
    private PDF getPDF() {
        final Intent intent = getIntent();
		Uri uri = intent.getData();    	
		filePath = uri.getPath();
		if (uri.getScheme().equals("file")) {
			if (history) {
				Recent recent = new Recent(this);
				recent.add(0, filePath);
				recent.commit();
			}
			return new PDF(new File(filePath), this.box);
    	} else if (uri.getScheme().equals("content")) {
    		ContentResolver cr = this.getContentResolver();
    		FileDescriptor fileDescriptor;
			try {
				fileDescriptor = cr.openFileDescriptor(uri, "r").getFileDescriptor();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e); // TODO: handle errors
			}
    		return new PDF(fileDescriptor, this.box);
    	} else {
    		throw new RuntimeException("don't know how to get filename from " + uri);
    	}
    }
    
	// event; @2013-07-24,9:33;
// #ifdef pro
    // * Handles back key by switching off text reflow mode if enabled.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    	    if (this.textReflowMode) {
    	    	this.setTextReflowMode(false);
    	    	return true; /* meaning we've handled event */
    	    }
    	}
    	return super.onKeyDown(keyCode, event);
    }
// #endif
 //optional menu; @2013-07-24,9:30;

	// Intercept touch events to handle the zoom buttons animation
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
    	int action = event.getAction();
    	if (action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_DOWN) {
	    	showPageNumber(true);
    		if (showZoomOnScroll) {
		    	showZoom();
	    	}
    	}
		return super.dispatchTouchEvent(event);    	
    };
    
    public boolean dispatchKeyEvent(KeyEvent event) 
	{
    	int action = event.getAction();
    	if (action==KeyEvent.ACTION_UP || action==KeyEvent.ACTION_DOWN) 
		{
    		if (!eink)
    			showAnimated(false);
    	}
		return super.dispatchKeyEvent(event);    	
    };
    
    public void showZoom() {
		// +ls @2013-07-23.17:56;
		boolean b = !( this.textReflowMode || zoomAnim == null );
		ls_bfshowZoomLayout( b );
		ls_showZoomLayout( b );
		ls_afshowZoomLayout( b );
		// -ls; @2013-07-23.18:09;
    }
    
    private void fadeZoom() {
		// +ls @2013-07-23.18:11;
		if( this.textReflowMode )
			ls_showZoomLayout( false );

		if( zoomAnim == null || eink )
			ls_showZoomLayout( false );
		else {
			ls_fadeZoomLayout();
		}
    }
    
    public void showPageNumber(boolean force) {
    	if (pageNumberAnim == null) {
    		pageNumberTextView.setVisibility(View.GONE);
    		return;
    	}
    	
    	pageNumberTextView.setVisibility(View.VISIBLE);
    	String newText = ""+(this.pagesView.getCurrentPage()+1)+"/"+
				this.pdfPagesProvider.getPageCount();
    	
    	if (!force && newText.equals(pageNumberTextView.getText()))
    		return;
    	
		pageNumberTextView.setText(newText);
    	pageNumberTextView.clearAnimation();

    	pageHandler.removeCallbacks(pageRunnable);
    	pageHandler.postDelayed(pageRunnable, fadeStartOffset);
    }
    
    private void fadePage() {
    	if (eink || pageNumberAnim == null) {
    		pageNumberTextView.setVisibility(View.GONE);
    	}
    	else {
    		pageNumberAnim.setStartOffset(0);
    		pageNumberAnim.setFillAfter(true);
    		pageNumberTextView.startAnimation(pageNumberAnim);
    	}
    }    
    
	//     * Show zoom buttons and page number
    public void showAnimated(boolean alsoZoom) {
    	if (alsoZoom)
    		showZoom();
    	showPageNumber(true);
    }
    
    // option menu; @2013-07-24,9:59;        
    
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      Log.i(TAG, "onConfigurationChanged(" + newConfig + ")");
    }
    
    // show find dialog @2013-07-24,10:04;
    
    private void setZoomLayout(SharedPreferences options) {
        
        ls_setColorMode(options); //@2013-07-24,10:16;
        ls_createZoomLayout( ); //@2013-07-23.18:23;
        setZoomButtonHandlers(); 		
		ls_addZoomLayout(); // @2013-07-23;20:13;        
        ls_showzoom();
   }
      
    // finder;
    // gui for finding text;    
    // show table contents dialog;    
    // onAccuracyChanged(); onSensorChanged();	
	// text reflow mode; &ls;	
	// nextpage();

//-----------------------------------------------------------------
//=== @2013-07-23;23:16; animation, fade ===
	private void ls_setFade(SharedPreferences options) {
		fadeStartOffset = 1000 * Integer.parseInt(options.getString(Options.PREF_FADE_SPEED, "7"));
	}
	private boolean ls_isZoomAnim() {
		return !(zoomAnim == null );
	}
	private void ls_setZoomAnim(SharedPreferences options) {
		int zoomAnimNumber = Integer.parseInt(options.getString(Options.PREF_ZOOM_ANIMATION, "2"));
		
		if (zoomAnimNumber == Options.ZOOM_BUTTONS_DISABLED)
			zoomAnim = null;
		else 
			zoomAnim = AnimationUtils.loadAnimation(this,
				zoomAnimations[zoomAnimNumber]);
	}
	private void ls_setPageNumAnim(SharedPreferences options) {
		//	SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);
		int pageNumberAnimNumber = Integer.parseInt(options.getString(Options.PREF_PAGE_ANIMATION, "3"));
		
		if (pageNumberAnimNumber == Options.PAGE_NUMBER_DISABLED)
			pageNumberAnim = null;
		else 
			pageNumberAnim = AnimationUtils.loadAnimation(this,
				pageNumberAnimations[pageNumberAnimNumber]);
	}
	private void ls_showPageNumText() {
		this.pageNumberTextView.setVisibility(pageNumberAnim == null ? View.GONE : View.VISIBLE);
	}
// === @2013-07-23;  click action; ===
	private void ls_setZoomButtonHandlers() {// called by setZoomButtonHandlers();
    // +ls, 2013-02-04;
    	this.btn_zoom_n1.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			ls_dobaraction( -1 );
			}
    	});
    	this.btn_zoom_n1.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				//ls_dobaraction( -100 );
				if( rzoom_show == 2)
    				pagesView.zoomFit();
    			//else
    			//	ls_dobaraction( -100 ); 
				return true;
			}
    	});
    	this.btn_zoom_n2.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			ls_dobaraction( -2 );
			}
    	});
    	this.btn_zoom_n2.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				//ls_dobaraction( -200 );
				return true;
			}
    	});
    	this.btn_zoom_n3.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			ls_dobaraction( -5 );
			}
    	});
    	this.btn_zoom_n3.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				//ls_dobaraction( -500 );
				return true;
			}
    	});
    	this.btn_zoom_n4.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			ls_dobaraction( -10 );
			}
    	});
    	this.btn_zoom_n4.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				//ls_dobaraction( -1000 );
				return true;
			}
    	});
    	this.btn_zoom_n5.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			ls_dobaraction( -20 );
			}
    	});
    	this.btn_zoom_n5.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				ls_dobaraction( -200 );
				return true;
			}
    	});
    	this.btn_zoom_n6.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			ls_dobaraction( -50 );
			}
    	});
    	this.btn_zoom_n6.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				ls_dobaraction( -500 );
				return true;
			}
    	});
    	this.btn_zoom_p1.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			ls_dobaraction( 1 );
			}
    	});
    	this.btn_zoom_p1.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				if( rzoom_show == 2)
    				pagesView.zoomWidth();
				return true;
			}
    	});
    	this.btn_zoom_p2.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			ls_dobaraction( 2 );
			}
    	});
    	this.btn_zoom_p2.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				//ls_dobaraction( 200 );
				return true;
			}
    	});
    	this.btn_zoom_p3.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			ls_dobaraction( 5 );
			}
    	});
    	this.btn_zoom_p3.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				//ls_dobaraction( 500 );
				return true;
			}
    	});
    	this.btn_zoom_p4.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			ls_dobaraction( 10 );
			}
    	});
    	this.btn_zoom_p4.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				//ls_dobaraction( 1000 );
				return true;
			}
    	});
    	this.btn_zoom_p5.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			ls_dobaraction( 20 );
			}
    	});
    	this.btn_zoom_p5.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				ls_dobaraction( 200 );
				return true;
			}
    	});
    	this.btn_zoom_p6.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			ls_dobaraction( 50 );
			}
    	});
    	this.btn_zoom_p6.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				ls_dobaraction( 500 );
				return true;
			}
    	});
    	this.btn_zoom_f.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {    			
    			//if( rzoom_show == 2)
    			//	pagesView.zoomFit();
    			//else
    				ls_dobaraction( -100 );    			
			}
    	});
    	this.btn_zoom_f.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				ls_dobaraction( -1000 );
				return true;
			}
    	});
    	this.btn_zoom_e.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {    			
    			//if( rzoom_show == 2)
    			//	pagesView.zoomWidth();
    			//else
    			//	ls_dobaraction( 100 );
    			ls_dobaraction( 100 );
			}
    	});
    	this.btn_zoom_e.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				ls_dobaraction( 1000 );
				return true;
			}
    	});
    	
    	this.btn_zoom_zbar.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
				//openOptionsMenu();
    			ls_clickshowzooms( 2 );
    			
			}
    	});
    	this.zoomWidthButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//pagesView.zoomWidth();
				ls_clickshowzooms( 3 );
			}
    	});
    	this.zoomWidthButton.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				//pagesView.zoomFit();
				return true;
			}
    	});
    	this.btn_zoom_rbar.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
				//openOptionsMenu();
    			ls_clickshowzooms( 1 );
			}
    	});
    	this.btn_zoom_rbar.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				// 2> set updowntap action; switch page/screen updown; @2013-07-23.17:31;
				pagesView.ls_setupdowntap();
				return true;
			}
    	});
    	
    	this.btn_zoom_t.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
				openOptionsMenu();
			}
    	});
    // -ls;
    	
    }
	private void ls_dobaraction( int n )
    {
    	if( rzoom_show == 1)
			ls_pagerelative(n);
    	else if( rzoom_show == 2)
			ls_zoomrelative(n);
    	else if( rzoom_show == 3 )
			ls_movehoriz( n );
    }
	private void ls_zoomrelative( int n )
    {
    	//pagesView.doAction(actions.getAction(Actions.ZOOM_IN));
    	pagesView.doActionZoom(n);
    }
    private void ls_movehoriz( int n )
    {
    	pagesView.ls_doScroll( -n*10, 0);
    }
    public void ls_pagerelative( int n )
    {
    	/*
    	//pagesView.zoomWidth();
		this.pagesView.scrollToPage(gopage, false);
		//this.pagesView.invalidate();
		 */
		int curpage = this.pagesView.getCurrentPage();
		int gopage = curpage + n;//2013-08-17; + 1;	// ls, 2013-02-06;
		if( gopage < 0 ) 
			gopage = 0;
		OpenFileActivity.this.gotoPage(gopage);
    }
//--- layout & show/hide zooms buttons; ---
	// note: 'zoomLayout' is the original created by apv;
	private void ls_setZoomLayout()
	{
		this.pagesView.setZoomLayout(zoomLayout);
		this.pagesView.setZoomLayout(zoomLayout2);
		this.pagesView.setZoomLayout(zoomLayout3);
	}
	private void ls_showZoomLayout_onresume() // not used. @2013-07-23.17:54;
	{
		if(isshowzoom == false)
		{
			this.zoomLayout.setVisibility(View.GONE);
			//(zoomAnim == null || this.textReflowMode) ? View.GONE : View.VISIBLE);
			this.zoomLayout2.setVisibility(View.GONE);
			//(zoomAnim == null || this.textReflowMode) ? View.GONE : View.VISIBLE);
			this.zoomLayout3.setVisibility(View.GONE);
			//(zoomAnim == null || this.textReflowMode) ? View.GONE : View.VISIBLE);
		}
	}
	private boolean ls_getIsshowzoom( boolean b)
	{
		return b && isshowzoom;
	}
	private void ls_bfshowZoomLayout( boolean b )
	{
		b = ls_getIsshowzoom( b );
		if( b ) {
			zoomLayout2.clearAnimation();
	    	zoomLayout3.clearAnimation();
	    	zoomLayout.clearAnimation();
		}
	}
	private void ls_setShowZoom( boolean b ) {
		isshowzoom = b;
	}
	public void ls_onShowHideZoom() {
		ls_setShowZoom(!isshowzoom);
		ls_showzoom();
	}
	public boolean ls_isShowZoom() {
		return isshowzoom;
	}
	private void ls_showzoom()
    {
		ls_showZoomLayout( isshowzoom);
		/* //@2013-07-23,22:55;
    	if( isshowzoom )
    	{
    		zoomLayout3.setVisibility(View.GONE);
    		zoomLayout2.setVisibility(View.GONE);
    		zoomLayout.setVisibility(View.GONE);
			//	isshowzoom = false;
    	}
    	else
    	{
    		zoomLayout.setVisibility(View.VISIBLE);
    		zoomLayout2.setVisibility(View.VISIBLE);
    		zoomLayout3.setVisibility(View.VISIBLE);
			//	isshowzoom = true;
    	}
*/
    }
	private void ls_showZoomLayout( boolean b )
	{
		b = ls_getIsshowzoom( b );
		if( b )
		{
			zoomLayout.setVisibility(View.VISIBLE);
    		zoomLayout2.setVisibility(View.VISIBLE);
    		zoomLayout3.setVisibility(View.VISIBLE);			
		}
		else
		{
			zoomLayout3.setVisibility(View.GONE);
    		zoomLayout2.setVisibility(View.GONE);
    		zoomLayout.setVisibility(View.GONE);
		}
		
	}
	private void ls_afshowZoomLayout( boolean b )
	{
		b = ls_getIsshowzoom( b );
		if ( b ) {
			zoomHandler.removeCallbacks(zoomRunnable);
	    	zoomHandler.postDelayed(zoomRunnable, fadeStartOffset);
		}
	}
	private void ls_fadeZoomLayout()
	{
		zoomAnim.setStartOffset(0);
    	zoomAnim.setFillAfter(true);

    	zoomLayout.startAnimation(zoomAnim);
    	// +ls;
    	zoomLayout2.startAnimation(zoomAnim);
    	zoomLayout3.startAnimation(zoomAnim);
    	// -ls;
	}
	private void ls_onsetTextReflowMode()
	{
		this.zoomLayout.clearAnimation();
  	    // +ls;
  	    this.zoomLayout2.clearAnimation();
  	    this.zoomLayout3.clearAnimation();
  	    // -ls;

  	    this.zoomHandler.removeCallbacks(zoomRunnable);

  		this.zoomLayout.setVisibility(View.GONE);
  		// +ls;
  		this.zoomLayout2.setVisibility(View.GONE);
  		this.zoomLayout3.setVisibility(View.GONE);
  		// -ls;
	}
	
	// called by btn_zoom_zbar/btn_zoom_rbar/zoomWidthButton:onClick();
	private void ls_clickshowzooms( int n) 
    {
    	if( rzoom_show == n)
			ShowZooms( 0 );
		else
			ShowZooms( n );
    }
    private void ShowZooms( int n )
    {
		if( n == 0 )	// hide it;
		{
			btn_zoom_n1.setVisibility(View.GONE);
			btn_zoom_p1.setVisibility(View.GONE);
			btn_zoom_n2.setVisibility(View.GONE);
			btn_zoom_p2.setVisibility(View.GONE);
			btn_zoom_n3.setVisibility(View.GONE);
			btn_zoom_p3.setVisibility(View.GONE);
			btn_zoom_n4.setVisibility(View.GONE);
			btn_zoom_p4.setVisibility(View.GONE);
			btn_zoom_n5.setVisibility(View.GONE);
			btn_zoom_p5.setVisibility(View.GONE);
			btn_zoom_n6.setVisibility(View.GONE);
			btn_zoom_p6.setVisibility(View.GONE);
			btn_zoom_f.setVisibility(View.GONE);
			btn_zoom_e.setVisibility(View.GONE);
		}
		else
		{
			btn_zoom_n1.setVisibility(View.VISIBLE);
			btn_zoom_p1.setVisibility(View.VISIBLE);
			btn_zoom_n2.setVisibility(View.VISIBLE);
			btn_zoom_p2.setVisibility(View.VISIBLE);
			btn_zoom_n3.setVisibility(View.VISIBLE);
			btn_zoom_p3.setVisibility(View.VISIBLE);
			btn_zoom_n4.setVisibility(View.VISIBLE);
			btn_zoom_p4.setVisibility(View.VISIBLE);
			btn_zoom_n5.setVisibility(View.VISIBLE);
			btn_zoom_p5.setVisibility(View.VISIBLE);
			btn_zoom_n6.setVisibility(View.VISIBLE);
			btn_zoom_p6.setVisibility(View.VISIBLE);
			btn_zoom_f.setVisibility(View.VISIBLE);
			btn_zoom_e.setVisibility(View.VISIBLE);
			
		}
		rzoom_show = n;
		
    }
	private void ls_createZoomLayout()
	{
		// the zoom buttons
    	if (zoomLayout != null) {
    		activityLayout.removeView(zoomLayout);
    	// +ls;
    		activityLayout.removeView(zoomLayout2);
    		activityLayout.removeView(zoomLayout3);
    	// -ls;
    	}
        zoomLayout = new LinearLayout(this);
        zoomLayout.setOrientation(LinearLayout.HORIZONTAL);
    // +LS; 2013-02-06;   
        
        zoomLayout2 = new LinearLayout(this);
        zoomLayout2.setOrientation(LinearLayout.HORIZONTAL);
        zoomLayout3 = new LinearLayout(this);
        zoomLayout3.setOrientation(LinearLayout.HORIZONTAL);
    // -ls;

        /*		
        zoomDownButton = new ImageButton(this);
		zoomDownButton.setImageDrawable(getResources().getDrawable(zoomDownId[mode]));
		zoomDownButton.setBackgroundColor(Color.TRANSPARENT);
		zoomLayout.addView(zoomDownButton, (int)(80 * metrics.density), (int)(50 * metrics.density));	// TODO: remove hardcoded values
		zoomWidthButton = new ImageButton(this);
		zoomWidthButton.setImageDrawable(getResources().getDrawable(zoomWidthId[mode]));
		zoomWidthButton.setBackgroundColor(Color.TRANSPARENT);
		zoomLayout.addView(zoomWidthButton, (int)(58 * metrics.density), (int)(50 * metrics.density));
		zoomUpButton = new ImageButton(this);		
		zoomUpButton.setImageDrawable(getResources().getDrawable(zoomUpId[mode]));
		zoomUpButton.setBackgroundColor(Color.TRANSPARENT);
		zoomLayout.addView(zoomUpButton, (int)(80 * metrics.density), (int)(50 * metrics.density));
         */		

    // +ls, 2013-02-04;           
        //btn_zoom_p1.setImageDrawable(getResources().getDrawable(zoomWidthId[mode]));
        //btn_zoom_p1.setBackgroundColor(Color.TRANSPARENT);
		btn_zoom_t = new Button( this);		//options menu
        btn_zoom_zbar = new Button( this);	//zoom bar;
 		zoomWidthButton = new Button(this);	//horization move;
        btn_zoom_rbar = new Button( this);	//read bar;
       
		btn_zoom_f = new Button(this);
		btn_zoom_e = new Button(this);
        btn_zoom_p1 = new Button(this);
		btn_zoom_p2 = new Button(this);
		btn_zoom_p3 = new Button(this);
		btn_zoom_p4 = new Button(this);		
		btn_zoom_p5 = new Button(this);
		btn_zoom_p6 = new Button(this);

		btn_zoom_n1 = new Button(this);//ImageButton(this);
		btn_zoom_n2 = new Button(this);
		btn_zoom_n3 = new Button(this);
		btn_zoom_n4 = new Button(this);
		btn_zoom_n5 = new Button(this);
		btn_zoom_n6 = new Button(this);

        zoomDownButton = new Button(this);
		zoomUpButton = new Button(this);

		btn_zoom_t.setText("MENU");		//btn_zoom_t.setTextColor(Color.BLACK);
		zoomWidthButton.setText("Move");//zoomWidthButton.setTextColor(Color.BLACK);
		btn_zoom_zbar.setText("Zoom");	//btn_zoom_zbar.setTextColor(Color.BLACK);
		btn_zoom_rbar.setText("Page");	//btn_zoom_rbar.setTextColor(Color.BLACK);
		//btn_zoom_rbar.setBackgroundColor(Color.DKGRAY);
		
		btn_zoom_n1.setText("-1");	//btn_zoom_n1.setBackgroundColor(Color.DKGRAY);//btn_zoom_n1.setTextColor(Color.BLUE);
		btn_zoom_n2.setText("-2");	//btn_zoom_n2.setBackgroundColor(Color.DKGRAY);//btn_zoom_n2.setTextColor(Color.BLUE);
		btn_zoom_n3.setText("-5");	//btn_zoom_n3.setBackgroundColor(Color.DKGRAY);//btn_zoom_n3.setTextColor(Color.BLUE);
		btn_zoom_n4.setText("-10");	//btn_zoom_n4.setBackgroundColor(Color.DKGRAY);//btn_zoom_n4.setTextColor(Color.BLUE);
		btn_zoom_n5.setText("-20");	//btn_zoom_n5.setBackgroundColor(Color.DKGRAY);//btn_zoom_n5.setTextColor(Color.BLUE);
		btn_zoom_n6.setText("-50");	//btn_zoom_n6.setBackgroundColor(Color.DKGRAY);//btn_zoom_n6.setTextColor(Color.BLUE);
		btn_zoom_p1.setText("1");	//btn_zoom_p1.setBackgroundColor(Color.DKGRAY);//btn_zoom_p1.setTextColor(Color.BLUE);
		btn_zoom_p2.setText("2");	//btn_zoom_p2.setBackgroundColor(Color.DKGRAY);//btn_zoom_p2.setTextColor(Color.BLUE);
		btn_zoom_p3.setText("5");	//btn_zoom_p3.setBackgroundColor(Color.DKGRAY);//btn_zoom_p3.setTextColor(Color.BLUE);
		btn_zoom_p4.setText("10");	//btn_zoom_p4.setBackgroundColor(Color.DKGRAY);//btn_zoom_p4.setTextColor(Color.BLUE);
		btn_zoom_p5.setText("20");	//btn_zoom_p5.setBackgroundColor(Color.DKGRAY);//btn_zoom_p5.setTextColor(Color.BLUE);
		btn_zoom_p6.setText("50");	//btn_zoom_p6.setBackgroundColor(Color.DKGRAY);//btn_zoom_p6.setTextColor(Color.BLUE);
		btn_zoom_f.setText("-100");	//btn_zoom_f.setBackgroundColor(Color.DKGRAY);//btn_zoom_f.setTextColor(Color.BLUE);
		btn_zoom_e.setText("100");	//btn_zoom_e.setBackgroundColor(Color.DKGRAY);//btn_zoom_e.setTextColor(Color.BLUE);
		
		// init show/hide;
		//if( rzoom_show > 0 )	// now = show; hide it;
		{
			btn_zoom_n1.setVisibility(View.GONE);
			btn_zoom_p1.setVisibility(View.GONE);
			btn_zoom_n2.setVisibility(View.GONE);
			btn_zoom_p2.setVisibility(View.GONE);
			btn_zoom_n3.setVisibility(View.GONE);
			btn_zoom_p3.setVisibility(View.GONE);
			btn_zoom_n4.setVisibility(View.GONE);
			btn_zoom_p4.setVisibility(View.GONE);
			btn_zoom_n5.setVisibility(View.GONE);
			btn_zoom_p5.setVisibility(View.GONE);
			btn_zoom_n6.setVisibility(View.GONE);
			btn_zoom_p6.setVisibility(View.GONE);
			btn_zoom_f.setVisibility(View.GONE);
			btn_zoom_e.setVisibility(View.GONE);
			rzoom_show = 0;
		}
		
		//zoomUpButton.setText("<-");	zoomUpButton.setTextColor(Color.BLUE);
		//zoomDownButton.setText("->");zoomDownButton.setTextColor(Color.BLUE);

	// +ls; 2013-02-06;
		DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
		zoomLayout2.addView(btn_zoom_p1, (int)(55 * metrics.density), (int)(50 * metrics.density));
		zoomLayout2.addView(btn_zoom_p2, (int)(55 * metrics.density), (int)(50 * metrics.density));		
		zoomLayout2.addView(btn_zoom_p3, (int)(55 * metrics.density), (int)(50 * metrics.density));		
		zoomLayout2.addView(btn_zoom_p4, (int)(55 * metrics.density), (int)(50 * metrics.density));
		zoomLayout2.addView(btn_zoom_p5, (int)(55 * metrics.density), (int)(50 * metrics.density));
		zoomLayout2.addView(btn_zoom_p6, (int)(55 * metrics.density), (int)(50 * metrics.density));
		zoomLayout2.addView(btn_zoom_e,  (int)(65 * metrics.density), (int)(50 * metrics.density));
		
		zoomLayout3.addView(btn_zoom_n1, (int)(55 * metrics.density), (int)(50 * metrics.density));	// fit=33;
		zoomLayout3.addView(btn_zoom_n2, (int)(55 * metrics.density), (int)(50 * metrics.density));		
		zoomLayout3.addView(btn_zoom_n3, (int)(55 * metrics.density), (int)(50 * metrics.density));
		zoomLayout3.addView(btn_zoom_n4, (int)(55 * metrics.density), (int)(50 * metrics.density));
		zoomLayout3.addView(btn_zoom_n5, (int)(55 * metrics.density), (int)(50 * metrics.density));
		zoomLayout3.addView(btn_zoom_n6, (int)(55 * metrics.density), (int)(50 * metrics.density));
		zoomLayout3.addView(btn_zoom_f,  (int)(65 * metrics.density), (int)(50 * metrics.density));
	// -ls;
			
		//zoomLayout.addView(zoomUpButton, (int)(80 * metrics.density), (int)(50 * metrics.density));	
		zoomLayout.addView(btn_zoom_t, (int)(75 * metrics.density), (int)(50 * metrics.density));	// MENU key;
		zoomLayout.addView(btn_zoom_zbar, (int)(75 * metrics.density), (int)(50 * metrics.density));
		zoomLayout.addView(zoomWidthButton, (int)(75 * metrics.density), (int)(50 * metrics.density));
		zoomLayout.addView(btn_zoom_rbar, (int)(75 * metrics.density), (int)(50 * metrics.density));
		//zoomLayout.addView(zoomDownButton, (int)(80 * metrics.density), (int)(50 * metrics.density));	// TODO: remove hardcoded values
// -LS;

		
	}
	private void ls_addZoomLayout()
	{
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
        		RelativeLayout.LayoutParams.WRAP_CONTENT, 
        		RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		zoomLayout.setId(1);
		
	// +ls; 2013-02-06;
		RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(
        		RelativeLayout.LayoutParams.WRAP_CONTENT, 
        		RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp2.addRule(RelativeLayout.CENTER_HORIZONTAL);//.ALIGN_PARENT_LEFT);
		lp2.addRule(RelativeLayout.ABOVE, 1);//.ALIGN_PARENT_TOP);
		zoomLayout2.setId(2);
		
		RelativeLayout.LayoutParams lp3 = new RelativeLayout.LayoutParams(
        		RelativeLayout.LayoutParams.WRAP_CONTENT, 
        		RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp3.addRule(RelativeLayout.CENTER_HORIZONTAL);//.ALIGN_PARENT_LEFT);
		lp3.addRule(RelativeLayout.ABOVE, 2);//.ALIGN_PARENT_TOP);
		
	// -ls;
		activityLayout.addView(zoomLayout,lp);
 	// +ls;
        activityLayout.addView(zoomLayout2,lp2);
        activityLayout.addView(zoomLayout3,lp3);
    // -ls;
	}

// === @2013-07-24;7:54; orientation ===
	private void ls_setOrient(SharedPreferences options) {
		sensorManager = null;

		if (Options.setOrientation(this)) {
			sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
			if (sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() > 0) {
				gravity[0] = 0f;
				gravity[1] = -9.81f;
				gravity[2] = 0f;
				gravityAge = 0;
				sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
						SensorManager.SENSOR_DELAY_NORMAL);
				this.prevOrientation = options.getInt(Options.PREF_PREV_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				setRequestedOrientation(this.prevOrientation);
			}
			else {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
		}
	}
	private void ls_setOrientOnPause() {
		if (sensorManager != null) {
			sensorManager.unregisterListener(this);
			sensorManager = null;
			SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
			edit.putInt(Options.PREF_PREV_ORIENTATION, prevOrientation);
			Log.v(TAG, "prevOrientation saved: "+prevOrientation);
			edit.commit();
		}
	}
	private void setOrientation(int orientation) {
    	if (orientation != this.prevOrientation) {
    		Log.v(TAG, "setOrientation: "+orientation);
    		setRequestedOrientation(orientation);
    		this.prevOrientation = orientation;
    	}
    }
	/**
     * Called when accuracy changes.
     * This method is empty, but it's required by relevant interface.
     */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public void onSensorChanged(SensorEvent event) {
		gravity[0] = 0.8f * gravity[0] + 0.2f * event.values[0];
		gravity[1] = 0.8f * gravity[1] + 0.2f * event.values[1];
		gravity[2] = 0.8f * gravity[2] + 0.2f * event.values[2];

		float sq0 = gravity[0]*gravity[0];
		float sq1 = gravity[1]*gravity[1];
		float sq2 = gravity[2]*gravity[2];
		
		gravityAge++;
		
		if (gravityAge < 4) {
			// ignore initial hiccups
			return;
		}
		
		if (sq1 > 3 * (sq0 + sq2)) {
			if (gravity[1] > 4) 
				setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			//else if (gravity[1] < -4 && Integer.parseInt(Build.VERSION.SDK) >= 9) 
			//	setOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
		}
		else if (sq0 > 3 * (sq1 + sq2)) {
			if (gravity[0] > 4)
				setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			//else if (gravity[0] < -4 && Integer.parseInt(Build.VERSION.SDK) >= 9) 
			//	setOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
		}
	}

	// === @2013-07-24;7:57; eink; ===
	private void ls_setEink(SharedPreferences options ) {
		boolean eink = options.getBoolean(Options.PREF_EINK, false);
		this.pagesView.setEink(eink);
		if (eink)
    		this.setTheme(android.R.style.Theme_Light);
		this.pagesView.setNook2(options.getBoolean(Options.PREF_NOOK2, false));

		this.eink = options.getBoolean(Options.PREF_EINK, false);
	}
	// === @2013-07-24;8:01; keep screen ===
	private void ls_setKeepScreen(SharedPreferences options) {
		if (options.getBoolean(Options.PREF_KEEP_ON, false))
			this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		else
			this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	//=== @2013-07-24,8:03; show zoom on scroll; ===
	private void ls_setShowZoomScroll(SharedPreferences options) {
		this.showZoomOnScroll = options.getBoolean(Options.PREF_SHOW_ZOOM_ON_SCROLL, false);
	}

	//===@2013-07-24,8:04; margins; ===
	private void ls_setMargin(SharedPreferences options) {
		this.pagesView.setSideMargins(
				Integer.parseInt(options.getString(Options.PREF_SIDE_MARGINS, "0")));
		this.pagesView.setTopMargin(
				Integer.parseInt(options.getString(Options.PREF_TOP_MARGIN, "0")));

	}

	//===@2013-07-24,8:06; double tap; ===
	private void ls_setDoubleTap(SharedPreferences options) {
		this.pagesView.setDoubleTap(Integer.parseInt(options.getString(Options.PREF_DOUBLE_TAP, 
				""+Options.DOUBLE_TAP_ZOOM_IN_OUT)));
	}

	//===@2013-07-24,8:09; box; ===
	private void ls_setBox(SharedPreferences options) {
		int newBox = Integer.parseInt(options.getString(Options.PREF_BOX, "2"));
		if (this.box != newBox) {
			saveLastPage();
			this.box = newBox;
	        startPDF(options);
	        this.pagesView.goToBookmark();
		}
	}
//=== @2013-07-24,10:12; color, image; ===

	private void ls_setColor(SharedPreferences options) {
		this.colorMode = Options.getColorMode(options);
		this.pageNumberTextView.setBackgroundColor(Options.getBackColor(colorMode));
        this.pageNumberTextView.setTextColor(Options.getForeColor(colorMode));
		this.pagesView.setColorMode(this.colorMode);
	}

	private void ls_setImages(SharedPreferences options) {
		this.pdfPagesProvider.setOmitImages(options.getBoolean(Options.PREF_OMIT_IMAGES, false));
	}

	private void ls_setColorMode(SharedPreferences options) {
		int colorMode = Options.getColorMode(options);
        int mode = ZOOM_COLOR_NORMAL;
        
        if (colorMode == Options.COLOR_MODE_GREEN_ON_BLACK) {
        	mode = ZOOM_COLOR_GREEN;
        }
        else if (colorMode == Options.COLOR_MODE_RED_ON_BLACK) {
        	mode = ZOOM_COLOR_RED;
        }
	}

// ===@2013-07-24,8:12; other settings; ===
	private void ls_setCache(SharedPreferences options) {
		this.pdfPagesProvider.setExtraCache(1024*1024*Options.getIntFromString(options, Options.PREF_EXTRA_CACHE, 0));
	}
	private void ls_setRender(SharedPreferences options) {
		this.pdfPagesProvider.setRenderAhead(options.getBoolean(Options.PREF_RENDER_AHEAD, true));
	}
	private void ls_setVerticalLock(SharedPreferences options) {
		this.pagesView.setVerticalScrollLock(options.getBoolean(Options.PREF_VERTICAL_SCROLL_LOCK, false));
	}
	//===@2013-07-24,8:22; history; ===
	private void ls_setHistory(SharedPreferences options) {
		history  = options.getBoolean(Options.PREF_HISTORY, true);
	}
	//===@2013-07-24,8:24; actions ===
	private void ls_setActions(SharedPreferences options) {
		actions = new Actions(options);
		this.pagesView.setActions(actions);
	}
	//===@2013-07-24,8:26; full screen; ===
	private void ls_setFullScreen(SharedPreferences options) {
		if (options.getBoolean(Options.PREF_FULLSCREEN, false))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		else
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

//----------------------------------------------------------------
//=== @2013-07-24, find; ===
    private void setFindButtonHandlers() 
	{
    	this.findPrevButton.setOnClickListener(
		new View.OnClickListener() {
			public void onClick(View v) {
				OpenFileActivity.this.findPrev();
			}
    	});
    	this.findNextButton.setOnClickListener(
		new View.OnClickListener() {
			public void onClick(View v) {
				OpenFileActivity.this.findNext();
			}
    	});
    	this.findHideButton.setOnClickListener(
        new View.OnClickListener() {
			public void onClick(View v) {
				OpenFileActivity.this.findHide();
			}
    	});
    }

	 private void findText(String text) {
    	Log.d(TAG, "findText(" + text + ")");
    	this.findText = text;
    	this.find(true);
    }

	//     * Hide the find buttons
    private void clearFind() {
		this.currentFindResultPage = null;
		this.currentFindResultNumber = null;
    	this.pagesView.setFindMode(false);
		this.findButtonsLayout.setVisibility(View.GONE);
    }
	//     * Called when user presses "next" button in find panel.
    private void findNext() {
    	this.find(true);
    }
	//     * Called when user presses "prev" button in find panel.
    private void findPrev() {
    	this.find(false);
    }
	//     * Called when user presses hide button in find panel.
    private void findHide() {
    	if (this.pagesView != null) this.pagesView.setFindMode(false);
    	this.currentFindResultNumber = null;
    	this.currentFindResultPage = null;
    	this.findButtonsLayout.setVisibility(View.GONE);
    }
	/**
     * Show find dialog.
     * Very pretty UI code ;)
     */
    public void ls_showFindDialog( int info )
    {
    	String s = String.valueOf(info);
    	Log.d(TAG, "find dialog...");
    	final Dialog dialog = new Dialog(this);
    	dialog.setTitle(s);//R.string.find_dialog_title);
    	LinearLayout contents = new LinearLayout(this);
    	contents.setOrientation(LinearLayout.VERTICAL);
    	this.findTextInputField = new EditText(this);
    	this.findTextInputField.setWidth(this.pagesView.getWidth() * 80 / 100);
    	Button goButton = new Button(this);
    	goButton.setText(R.string.find_go_button);
    	goButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String text = OpenFileActivity.this.findTextInputField.getText().toString();
				OpenFileActivity.this.findText(text);
				dialog.dismiss();
			}
    	});
    	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    	params.leftMargin = 5;
    	params.rightMargin = 5;
    	params.bottomMargin = 2;
    	params.topMargin = 2;
    	contents.addView(findTextInputField, params);
    	contents.addView(goButton, params);
    	dialog.setContentView(contents);
    	dialog.show();
    }
    public void showFindDialog() {
    	Log.d(TAG, "find dialog...");
    	final Dialog dialog = new Dialog(this);
    	dialog.setTitle(R.string.find_dialog_title);
    	LinearLayout contents = new LinearLayout(this);
    	contents.setOrientation(LinearLayout.VERTICAL);
    	this.findTextInputField = new EditText(this);
    	this.findTextInputField.setWidth(this.pagesView.getWidth() * 80 / 100);
    	Button goButton = new Button(this);
    	goButton.setText(R.string.find_go_button);
    	goButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String text = OpenFileActivity.this.findTextInputField.getText().toString();
				OpenFileActivity.this.findText(text);
				dialog.dismiss();
			}
    	});
    	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    	params.leftMargin = 5;
    	params.rightMargin = 5;
    	params.bottomMargin = 2;
    	params.topMargin = 2;
    	contents.addView(findTextInputField, params);
    	contents.addView(goButton, params);
    	dialog.setContentView(contents);
    	dialog.show();
    }

	/**
     * Helper class that handles search progress, search cancelling etc.
     */
	static class Finder implements Runnable, DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
		private OpenFileActivity parent = null;
		private boolean forward;
		private AlertDialog dialog = null;
		private String text;
		private int startingPage;
		private int pageCount;
		private boolean cancelled = false;
		/**
		 * Constructor for finder.
		 * @param parent parent activity
		 */
		public Finder(OpenFileActivity parent, boolean forward) {
			this.parent = parent;
			this.forward = forward;
			this.text = parent.findText;
			this.pageCount = parent.pagesView.getPageCount();
			if (parent.currentFindResultPage != null) {
				if (forward) {
					this.startingPage = (parent.currentFindResultPage + 1) % pageCount;
				} else {
					this.startingPage = (parent.currentFindResultPage - 1 + pageCount) % pageCount;
				}
			} else {
				this.startingPage = parent.pagesView.getCurrentPage();
			}
		}
		public void setDialog(AlertDialog dialog) {
			this.dialog = dialog;
		}
		public void run() {
			int page = -1;
			this.createDialog();
			this.showDialog();
			for(int i = 0; i < this.pageCount; ++i) {
				if (this.cancelled) {
					this.dismissDialog();
					return;
				}
				page = (startingPage + pageCount + (this.forward ? i : -i)) % this.pageCount;
				Log.d(TAG, "searching on " + page);
				this.updateDialog(page);
				List<FindResult> findResults = this.findOnPage(page);
				if (findResults != null && !findResults.isEmpty()) {
					Log.d(TAG, "found something at page " + page + ": " + findResults.size() + " results");
					this.dismissDialog();
					this.showFindResults(findResults, page);
					return;
				}
			}
			/* TODO: show "nothing found" message */
			this.dismissDialog();
		}
		/**
		 * Called by finder thread to get find results for given page.
		 * Routed to PDF instance.
		 * If result is not empty, then finder loop breaks, current find position
		 * is saved and find results are displayed.
		 * @param page page to search on
		 * @return results 
		 */
		private List<FindResult> findOnPage(int page) {
			if (this.text == null) throw new IllegalStateException("text cannot be null");
			return this.parent.pdf.find(this.text, page, this.parent.pagesView.getPageRotation());
		}

		private void createDialog() {
			this.parent.runOnUiThread(new Runnable() {
				public void run() {
					String title = Finder.this.parent.getString(R.string.searching_for).replace("%1$s", Finder.this.text);
					String message = Finder.this.parent.getString(R.string.page_of).replace("%1$d", String.valueOf(Finder.this.startingPage)).replace("%2$d", String.valueOf(pageCount));
			    	AlertDialog.Builder builder = new AlertDialog.Builder(Finder.this.parent);
			    	AlertDialog dialog = builder
			    		.setTitle(title)
			    		.setMessage(message)
			    		.setCancelable(true)
			    		.setNegativeButton(R.string.cancel, Finder.this)
			    		.create();
			    	dialog.setOnCancelListener(Finder.this);
			    	Finder.this.dialog = dialog;
				}
			});
		}
		public void updateDialog(final int page) {
			this.parent.runOnUiThread(new Runnable() {
				public void run() {
					String message = Finder.this.parent.getString(R.string.page_of).replace("%1$d", String.valueOf(page)).replace("%2$d", String.valueOf(pageCount));
					Finder.this.dialog.setMessage(message);
				}
			});
		}
		public void showDialog() {
			this.parent.runOnUiThread(new Runnable() {
				public void run() {
					Finder.this.dialog.show();
				}
			});
		}
		public void dismissDialog() {
			final AlertDialog dialog = this.dialog;
			this.parent.runOnUiThread(new Runnable() {
				public void run() {
					dialog.dismiss();
				}
			});
		}
		public void onCancel(DialogInterface dialog) {
			Log.d(TAG, "onCancel(" + dialog + ")");
			this.cancelled = true;
		}
		public void onClick(DialogInterface dialog, int which) {
			Log.d(TAG, "onClick(" + dialog + ")");
			this.cancelled = true;
		}
		private void showFindResults(final List<FindResult> findResults, final int page) {
			this.parent.runOnUiThread(new Runnable() {
				public void run() {
					int fn = Finder.this.forward ? 0 : findResults.size()-1;
					Finder.this.parent.currentFindResultPage = page;
					Finder.this.parent.currentFindResultNumber = fn;
					Finder.this.parent.pagesView.setFindResults(findResults);
					Finder.this.parent.pagesView.setFindMode(true);
					Finder.this.parent.pagesView.scrollToFindResult(fn);
					Finder.this.parent.findButtonsLayout.setVisibility(View.VISIBLE);					
					Finder.this.parent.pagesView.ls_invalidate();
				}
			});
		}
	};

	/**
     * GUI for finding text.
     * Used both on initial search and for "next" and "prev" searches.
     * Displays dialog, handles cancel button, hides dialog as soon as
     * something is found.
     * @param 
     */
    private void find(boolean forward) {
    	if (this.currentFindResultPage != null) {
    		/* searching again */
    		int nextResultNum = forward ? this.currentFindResultNumber + 1 : this.currentFindResultNumber - 1;
    		if (nextResultNum >= 0 && nextResultNum < this.pagesView.getFindResults().size()) {
    			/* no need to really find - just focus on given result and exit */
    			this.currentFindResultNumber = nextResultNum;
    			this.pagesView.scrollToFindResult(nextResultNum);
    			this.pagesView.ls_invalidate();
    			return;
    		}
    	}

    	/* finder handles next/prev and initial search by itself */
    	Finder finder = new Finder(this, forward);
    	Thread finderThread = new Thread(finder);
    	finderThread.start();
    }

//=== Set handlers on zoom level buttons ===
	// codes has moved to the end of this file by ls @2013-07-23; 16:59;
	private void setZoomButtonHandlers() 
    {

		ls_setZoomButtonHandlers();

		this.zoomDownButton.setOnLongClickListener(
        new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				//pagesView.doAction(actions.getAction(Actions.LONG_ZOOM_IN));
				//pagesView.doAction(Actions.ACTION_SCREEN_DOWN);
				OpenFileActivity.this.gotoPage(pagesView.getPageCount() -1);
				return true;
			}
    	});
    	this.zoomDownButton.setOnClickListener(
        new View.OnClickListener() {
			public void onClick(View v) {
				//pagesView.doAction(actions.getAction(Actions.ZOOM_IN));
				pagesView.doAction(Actions.ACTION_SCREEN_DOWN);
			}
    	});
    	this.zoomUpButton.setOnClickListener(
        new View.OnClickListener() {
			public void onClick(View v) {
				//pagesView.doAction(actions.getAction(Actions.ZOOM_OUT));
				pagesView.doAction(Actions.ACTION_SCREEN_UP);
			}
    	});
    	this.zoomUpButton.setOnLongClickListener(
        new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				//pagesView.doAction(actions.getAction(Actions.LONG_ZOOM_OUT));
				//pagesView.doAction(Actions.ACTION_SCREEN_UP);
				OpenFileActivity.this.gotoPage(0);
				
				return true;
			}
    	}); 
	}
	//    }
    
    /**
     * Show error message to user.
     * @param message message to show
     */
    private void errorMessage(String message) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	AlertDialog dialog = builder.setMessage(message).setTitle("Error").create();
    	dialog.show();
    }
//=== @2013-07-24,9:35 page operation; ===
	private void gotoPage(int page) {
    	Log.i(TAG, "rewind to page " + page);
    	if (this.pagesView != null) {
    		this.pagesView.scrollToPage(page);
			showAnimated(true);
    	}
    }
    
   /**
     * Save the last page in the bookmarks
     */
    private void saveLastPage() {
    	BookmarkEntry entry = this.pagesView.toBookmarkEntry();
        Bookmark b = new Bookmark(this.getApplicationContext()).open();
        b.setLast(filePath, entry);
        b.close();
        Log.i(TAG, "last page saved for "+filePath);    
    }

// #ifdef pro 	
	/**
	 * Change to next or prev page.
	 * Called from text reflow mode buttons.
	 * @param offset if 1 then go to next page, if -1 then go to prev page, otherwise raise IllegalArgumentException
	 */
	private void nextPage(int offset) {
		if (offset == 1) {
			this.pagesView.doAction(Actions.ACTION_FULL_PAGE_DOWN);
		} else if (offset == -1) {
			this.pagesView.doAction(Actions.ACTION_FULL_PAGE_UP);
		} else {
			throw new IllegalArgumentException("invalid offset: " + offset);
		}
		if (this.textReflowMode) {
			int page = this.pagesView.getCurrentPage();
			String text = this.pdf.getText(page);
			if (text == null) text = "";
			text = text.trim();
			this.textReflowTextView.setText(text);
			this.textReflowScrollView.scrollTo(0,0);
		}
//
	}
// #endif

    /**
     * Called from menu when user want to go to specific page.
     */
    private void showGotoPageDialog() 
	{
    	final Dialog d = new Dialog(this);
    	d.setTitle(R.string.goto_page_dialog_title);
    	LinearLayout contents = new LinearLayout(this);
    	contents.setOrientation(LinearLayout.VERTICAL);
    	TextView label = new TextView(this);
    	final int pagecount = this.pdfPagesProvider.getPageCount();
    	label.setText("Page number from " + 1 + " to " + pagecount);
    	this.pageNumberInputField = new EditText(this);
    	this.pageNumberInputField.setInputType(InputType.TYPE_CLASS_NUMBER);
    	this.pageNumberInputField.setText("" + (this.pagesView.getCurrentPage() + 1));
    	Button goButton = new Button(this);
    	goButton.setText(R.string.goto_page_go_button);
    	goButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				int pageNumber = -1;
				try {
					pageNumber = Integer.parseInt(OpenFileActivity.this.pageNumberInputField.getText().toString())-1;
				} catch (NumberFormatException e) {
					/* ignore */
				}
				d.dismiss();
				if (pageNumber >= 0 && pageNumber < pagecount) {
					OpenFileActivity.this.gotoPage(pageNumber);

				} else {
					OpenFileActivity.this.errorMessage("Invalid page number");
				}
			}
    	});
    	Button page1Button = new Button(this);
    	page1Button.setText(getResources().getString(R.string.page) +" 1");
    	page1Button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				d.dismiss();
				OpenFileActivity.this.gotoPage(0);
			}
    	});
    	Button lastPageButton = new Button(this);
    	lastPageButton.setText(getResources().getString(R.string.page) +" "+pagecount);
    	lastPageButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				d.dismiss();
				OpenFileActivity.this.gotoPage(pagecount-1);
			}
    	});
    	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    	params.leftMargin = 5;
    	params.rightMargin = 5;
    	params.bottomMargin = 2;
    	params.topMargin = 2;
    	contents.addView(label, params);
    	contents.addView(pageNumberInputField, params);
    	contents.addView(goButton, params);
    	contents.addView(page1Button, params);
    	contents.addView(lastPageButton, params);
    	d.setContentView(contents);
    	d.show();
    }

	//=== @2013-07-24,9:29; optional menu operation ===

	//     * Create options menu, called by Android system.
    // * @param menu menu to populate
    // * @return true meaning that menu was populated

// +ls, 2013-02-05;
    @Override
    public void openOptionsMenu() {
     // TODO Auto-generated method stub
     super.openOptionsMenu();
    }
// -ls;  
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	
    	//+ls: 140302;
    	mlsmenu.Add2Menu(menu); //
    	// #ifdef pro
    	//mls: 140302;
    	this.tableOfContentsMenuItem = menu.add(R.string.table_of_contents);
    	// #endif
    	this.optionsMenuItem = menu.add(R.string.options);
    	this.gotoPageMenuItem = menu.add(R.string.goto_page);
		this.findTextMenuItem = menu.add(R.string.find_text);
    	this.clearFindTextMenuItem = menu.add(R.string.clear_find_text);
    	this.chooseFileMenuItem = menu.add(R.string.choose_file);
    	/* The following appear on the second page.  The find item can safely be kept
    	 * there since it can also be accessed from the search key on most devices.
    	 */
    	
    	// #ifdef pro
//mls:140302;    	this.tableOfContentsMenuItem = menu.add(R.string.table_of_contents);
    	this.textReflowMenuItem = menu.add(R.string.text_reflow);
    	// #endif
    	this.rotateRightMenuItem = menu.add(R.string.rotate_page_left);
    	this.rotateLeftMenuItem = menu.add(R.string.rotate_page_right);
    	this.aboutMenuItem = menu.add(R.string.about);
    	return true;
    }

	/**
     * Prepare menu contents.
     * Hide or show "Clear find results" menu item depending on whether
     * we're in find mode.
     * @param menu menu that should be prepared
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	this.clearFindTextMenuItem.setVisible(this.pagesView.getFindMode());
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
    	if (menuItem == this.aboutMenuItem) {
			Intent intent = new Intent();
			intent.setClass(this, AboutPDFViewActivity.class);
			this.startActivity(intent);
    		return true;
    	} 
    	// +ls: 140302;
    	else if( this.mlsmenu.isLsbarMenu(menuItem) ){
    		this.ls_onShowHideZoom();
    	}
    	
    	else if (menuItem == this.gotoPageMenuItem) {
    		this.showGotoPageDialog();
    	} else if (menuItem == this.rotateLeftMenuItem) {
    		this.pagesView.rotate(-1);
    	} else if (menuItem == this.rotateRightMenuItem) {
    		this.pagesView.rotate(1);
    	} else if (menuItem == this.findTextMenuItem) {
    		this.showFindDialog();
    	} else if (menuItem == this.clearFindTextMenuItem) {
    		this.clearFind();
    	} else if (menuItem == this.chooseFileMenuItem) {
    		startActivity(new Intent(this, ChooseFileActivity.class));
    	} else if (menuItem == this.optionsMenuItem) {
    		startActivity(new Intent(this, Options.class));
    	// #ifdef pro
		} else if (menuItem == this.tableOfContentsMenuItem) {
			Outline outline = this.pdf.getOutline();
			if (outline != null) {
				this.showTableOfContentsDialog(outline);
			} else {
				Toast.makeText(this, "Table of Contents not found", Toast.LENGTH_SHORT).show();
			}
		} else if (menuItem == this.textReflowMenuItem) {
			this.setTextReflowMode(! this.textReflowMode);

		// #endif
		}
    	return false;
    }

	//=== @2013-07-24,10:21 content table ===
	// #ifdef pro
    /**
     * Build and display dialog containing table of contents.
     * @param outline root of TOC tree
     */
// !ls; 2013-02-05;    
    private void showTableOfContentsDialog(Outline outline) {
    	if (outline == null) throw new IllegalArgumentException("nothing to show");
    	final Dialog dialog = new Dialog(this);
    	dialog.setTitle(R.string.toc_dialog_title);
    	LinearLayout contents = new LinearLayout(this);
    	contents.setOrientation(LinearLayout.VERTICAL);
// +ls; 2013-02-05;    	
    	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    	//LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
// -ls;    	
    	params.leftMargin = 5;
    	params.rightMargin = 5;
    	params.bottomMargin = 2;
    	params.topMargin = 2;
    	final TreeView tocTree = new TreeView(this);
    	tocTree.setCacheColorHint(0);
    	tocTree.setTree(outline);
    	DocumentOptions documentOptions = new DocumentOptions(this.getApplicationContext());
    	try {
	    	String openNodesString = documentOptions.getValue(this.filePath, "toc_open_nodes");
	    	if (openNodesString != null) {
		    	String[] openNodes = documentOptions.getValue(this.filePath, "toc_open_nodes").split(",");
		    	for(String openNode: openNodes) {
		    		long nodeId = -1;
		    		try {
		    			nodeId = Long.parseLong(openNode);
		    		} catch (NumberFormatException e) {
		    			Log.w(TAG, "failed to parse " + openNode + " as long: " + e);
		    			continue;
		    		}
		    		tocTree.open(nodeId);
		    	}
	    	}
    	} finally {
    		documentOptions.close();
    	}
    	tocTree.setOnItemClickListener(new OnItemClickListener() {
    		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    			Log.d(TAG, "onItemClick(" + parent + ", " + view + ", " + position + ", " + id);
    			TreeView treeView = (TreeView)parent;
    			TreeView.TreeNode treeNode = treeView.getTreeNodeAtPosition(position);
    			Outline outline = (Outline) treeNode;
    			int pageNumber = outline.page;
    			OpenFileActivity.this.gotoPage(pageNumber);
    			dialog.dismiss();
    		}
    	});
    	contents.addView(tocTree, params);
    	dialog.setContentView(contents);
    	dialog.setOnDismissListener(new OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				/* save state */
				Log.d(TAG, "saving TOC tree state");
				Map<Long,Boolean> state = tocTree.getState();
    			String openNodes = "";
    			for(long key: state.keySet()) {
    				if (state.get(key)) {
    					if (openNodes.length() > 0) openNodes += ",";
    					openNodes += key;
    				}
    			}
    			DocumentOptions documentOptions = new DocumentOptions(OpenFileActivity.this.getApplicationContext());
    			try {
    				documentOptions.setValue(filePath, "toc_open_nodes", openNodes);
    			} finally {
    				documentOptions.close();
    			}
			}
    	});
    	dialog.show();
    }
	// #endif

//=== @2013-07-24,10:23; text reflow mode; ===
	// #ifdef pro
  	/**
  	 * Switch text reflow mode and set this.textReflowMode by hiding and showing relevant interface elements.
  	 * @param mode if true ten show text reflow view, otherwise hide text reflow view
  	 */
  	private void setTextReflowMode(boolean mode) {
  		if (mode) {
  			Log.d(TAG, "text reflow");
  			int page = this.pagesView.getCurrentPage();
  			String text = this.pdf.getText(page);
  			if (text == null) text = "";
  			text = text.trim();
  			Log.d(TAG, "text of page " + page + " is: " + text);
  			this.textReflowTextView.setText(text);
  			this.textReflowScrollView.scrollTo(0,0);
  			this.textReflowMenuItem.setTitle("Close Text Reflow");
  			this.pagesView.setVisibility(View.GONE);
  	    	
			ls_onsetTextReflowMode(); // @2013-07-23.18:28;

  			this.textReflowView.setVisibility(View.VISIBLE);
  			this.textReflowMode = true;
  		} else {
  			this.textReflowMenuItem.setTitle("Text Reflow");
  			this.textReflowView.setVisibility(View.GONE);
  			this.pagesView.setVisibility(View.VISIBLE);
  			this.textReflowMode = false;
  			this.showZoom();
  		}
  	}
// #endif

}
