package com.programmersgateway.sm1999.sketchtab;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.LayoutDirection;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;

import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity {

    MainActivity.DrawingView dv;
    private Paint mPaint;
    int initialColor;

    FileOutputStream fos = null;


    public int width;
    public int height;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    Context context;
    private Paint circlePaint;
    private Path circlePath;
    Boolean newAdded = false;
    Boolean allClear = false;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private boolean isSaved = false;
    private ArrayList<Bitmap> bitmap = new ArrayList<>();
    private ArrayList<Bitmap> undoBitmap = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dv = new MainActivity.DrawingView(this);
        dv.setBackgroundColor(Color.WHITE);
        setContentView(dv);

        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("");

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.BLUE);
        initialColor = Color.BLUE;

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);


    }


    public class DrawingView extends View {

        public DrawingView(Context c) {
            super(c);
            context = c;
            mPath = new Path();
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
            circlePaint = new Paint();
            circlePath = new Path();
            circlePaint.setAntiAlias(true);
            circlePaint.setColor(Color.BLUE);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeJoin(Paint.Join.MITER);
            circlePaint.setStrokeWidth(4f);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);

            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath(mPath, mPaint);
            canvas.drawPath(circlePath, circlePaint);

        }

        private float mX, mY;
        private static final float TOUCH_TOLERANCE = 4;

        private void touch_start(float x, float y) {
            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            mY = y;
        }

        private void touch_move(float x, float y) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                mX = x;
                mY = y;

                circlePath.reset();
                circlePath.addCircle(mX, mY, 30, Path.Direction.CW);
            }
        }

        private void touch_up() {
            mPath.lineTo(mX, mY);
            circlePath.reset();
            // commit the path to our offscreen
            mCanvas.drawPath(mPath, mPaint);
            // kill this so we don't double draw
            mPath.reset();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    newAdded = true;
                    if (!allClear)
                        bitmap.add(mBitmap.copy(mBitmap.getConfig(), mBitmap.isMutable()));
                    else allClear = false;
                    touch_start(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    invalidate();
                    break;
            }
            return true;
        }

        public void onClickUndo() {
            if (newAdded) {
                bitmap.add(mBitmap.copy(mBitmap.getConfig(), mBitmap.isMutable()));
                newAdded = false;
            }
            if (bitmap.size() > 1) {
                undoBitmap.add(bitmap.remove(bitmap.size() - 1));
                mBitmap = bitmap.get(bitmap.size() - 1).copy(mBitmap.getConfig(), mBitmap.isMutable());
                mCanvas = new Canvas(mBitmap);
                invalidate();
                if (bitmap.size() == 1)
                    allClear = true;
            }
        }

        public void onClickRedo() {
            if (undoBitmap.size() > 0) {
                bitmap.add(undoBitmap.remove(undoBitmap.size() - 1));
                mBitmap = bitmap.get(bitmap.size() - 1).copy(mBitmap.getConfig(), mBitmap.isMutable());
                mCanvas = new Canvas(mBitmap);
                invalidate();
            }
        }

    }

    public Bitmap getBitmap(View view) {

        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.setBitmap(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.color_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_name) {
            final AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, initialColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                @Override
                public void onOk(AmbilWarnaDialog dialog, int color) {
                    // color is the color selected by the user.
                    mPaint.setColor(color);
                    initialColor = color;
                }

                @Override
                public void onCancel(AmbilWarnaDialog dialog) {
                    // cancel was selected by the user
                }
            });
            dialog.show();
            return true;
        }
        if (id == R.id.erase) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeWidth(30);
            mPaint.setColor(Color.WHITE);
        }
        if (id == R.id.clear) {
            mCanvas.drawColor(Color.WHITE);
            mPaint.setColor(initialColor);
            isSaved = false;
        }
        if (id == R.id.brush) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeWidth(50);
            mPaint.setColor(initialColor);

        }
        if (id == R.id.pen) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeWidth(10);
            mPaint.setColor(initialColor);
        }
        if (id == R.id.action_save) {
            verifyStoragePermissions(MainActivity.this);
            saveImageToGallery(getBitmap(dv));
        }
        if (id == R.id.undo) {
            dv.onClickUndo();
        }
        if (id == R.id.redo) {
            dv.onClickRedo();
        }
        if (id == R.id.action_shapes) {
            showShapesDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    private void drawCircle(int radius, boolean filled) {

        if (filled)
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        else {
            mPaint.setStyle(Paint.Style.STROKE);
        }
        mCanvas.drawCircle(dv.getWidth() / 2, dv.getHeight() / 2, radius, mPaint);

    }

    private void drawRectangle(int inputWidth, int inputHeight, boolean filled) {


        if (filled)
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        else {
            mPaint.setStyle(Paint.Style.STROKE);
        }
        int top = dv.getHeight() / 2 - 50;
        int left = dv.getWidth() / 4 + 100;
        mCanvas.drawRect(new Rect(left, top, left + inputWidth, top + inputHeight), mPaint);

    }

    private void drawSquare(int side, boolean filled) {

        if (filled)
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        else {
            mPaint.setStyle(Paint.Style.STROKE);
        }
        int top = dv.getHeight() / 2 - 50;
        int left = dv.getWidth() / 4 + 100;
        mCanvas.drawRect(new Rect(left, top, left + side, top + side), mPaint);
    }

    private void drawTriangle(boolean filled) {

        if (filled)
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        else {
            mPaint.setStyle(Paint.Style.STROKE);
        }
        Point point1_draw = new Point(100, dv.getHeight() / 2); //LB - A
        Point point2_draw = new Point(500, dv.getHeight() / 2); //RB - B
        Point point3_draw = new Point((point1_draw.x + point2_draw.x) / 2, 200); //TOP - C

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(point1_draw.x, point1_draw.y);
        path.lineTo(point2_draw.x, point2_draw.y);
        path.lineTo(point3_draw.x, point3_draw.y);
        path.lineTo(point1_draw.x, point1_draw.y);
        path.close();

        mCanvas.drawPath(path, mPaint);
    }

    private void showShapesDialog() {
        String[] shapes = {"No shape selected", "Circle", "Square", "Rectangle", "Triangle"};
        final ArrayAdapter<String> shapesAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, shapes);

        final Spinner shapesDropdown = new Spinner(MainActivity.this);
        final ScrollView scrollView = new ScrollView(MainActivity.this);
        final LinearLayout layout = new LinearLayout(MainActivity.this);
        final LinearLayout.LayoutParams dialogLayoutParams = new LinearLayout.LayoutParams(ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.MATCH_PARENT);
        dialogLayoutParams.setMargins(20, 10, 20, 10);
        shapesDropdown.setId(R.id.shapesdropdown);
        shapesDropdown.setPadding(30, 30, 30, 30);
        shapesDropdown.setBackground(getDrawable(R.drawable.gradient_spinner));
        shapesDropdown.setAdapter(shapesAdapter);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(dialogLayoutParams);
        layout.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.END);
        layout.addView(shapesDropdown, dialogLayoutParams);
        scrollView.addView(layout);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Select a Shape to Draw");
        builder.setMessage("Select a shape from the dropdown to draw on the canvas.");
        builder.setView(scrollView);
        final AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dialog.getWindow().setLayout(650, 600);
        }


        shapesDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (shapesDropdown.getSelectedItem().toString().equals("Circle")) {
                    layout.removeAllViewsInLayout();
                    layout.invalidate();
                    layout.addView(shapesDropdown, dialogLayoutParams);
                    dialog.getWindow().setLayout(650, 800);


                    final EditText radius = new EditText(MainActivity.this);
                    radius.setVisibility(View.VISIBLE);
                    radius.setPadding(30, 30, 30, 30);
                    radius.setHint("Enter a radius");
                    radius.setInputType(InputType.TYPE_CLASS_NUMBER);
                    radius.requestFocus();
                    radius.setId(R.id.radius);
                    layout.addView(radius, dialogLayoutParams);

                    final RadioGroup fillColorGroup = new RadioGroup(MainActivity.this);
                    fillColorGroup.setLayoutParams(new RadioGroup.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    fillColorGroup.setOrientation(LinearLayout.HORIZONTAL);
                    fillColorGroup.setId(R.id.rgcircle);
                    final RadioButton filled = new RadioButton(MainActivity.this);
                    RadioButton not_filled = new RadioButton(MainActivity.this);
                    filled.setLayoutParams(new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT, 1f));
                    not_filled.setLayoutParams(new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT, 1f));
                    filled.setId(R.id.filled);
                    filled.setText("Filled");
                    not_filled.setId(R.id.not_filled);
                    not_filled.setText("Not Filled");
                    fillColorGroup.addView(filled);
                    fillColorGroup.addView(not_filled);
                    layout.addView(fillColorGroup, dialogLayoutParams);

                    final Button submit = new Button(MainActivity.this);
                    submit.setText(R.string.submit);
                    submit.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    submit.setVisibility(View.VISIBLE);
                    submit.setPadding(30, 30, 30, 30);
                    submit.setId(R.id.submit);
                    layout.addView(submit, dialogLayoutParams);


                    submit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!radius.getText().toString().equals("")) {
                                if (Integer.parseInt(radius.getText().toString()) <= 350) {
                                    if (fillColorGroup.getCheckedRadioButtonId() == filled.getId()) {
                                        drawCircle(Integer.parseInt(radius.getText().toString()), true);
                                    } else {
                                        drawCircle(Integer.parseInt(radius.getText().toString()), false);
                                    }
                                    dialog.dismiss();
                                } else {
                                    radius.setError("Circle with given radius exceeds the bounds of visible screen. Please give a valid radius.");
                                }
                            } else
                                radius.setError("Please enter a radius");
                        }
                    });


                } else if (shapesDropdown.getSelectedItem().toString().equals("Rectangle")) {
                    layout.removeAllViewsInLayout();
                    layout.invalidate();
                    layout.addView(shapesDropdown, dialogLayoutParams);
                    dialog.getWindow().setLayout(650, 900);


                    final EditText length = new EditText(MainActivity.this);
                    length.setVisibility(View.VISIBLE);
                    length.setPadding(30, 30, 30, 30);
                    length.setHint("Enter length");
                    length.setInputType(InputType.TYPE_CLASS_NUMBER);
                    length.requestFocus();
                    length.setId(R.id.length);
                    layout.addView(length, dialogLayoutParams);

                    final EditText breadth = new EditText(MainActivity.this);
                    breadth.setVisibility(View.VISIBLE);
                    breadth.setPadding(30, 30, 30, 30);
                    breadth.setHint("Enter breadth");
                    breadth.setInputType(InputType.TYPE_CLASS_NUMBER);
                    breadth.requestFocus();
                    breadth.setId(R.id.breadth);
                    layout.addView(breadth, dialogLayoutParams);

                    final RadioGroup fillColorGroup = new RadioGroup(MainActivity.this);
                    fillColorGroup.setLayoutParams(new RadioGroup.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    fillColorGroup.setOrientation(LinearLayout.HORIZONTAL);
                    fillColorGroup.setId(R.id.rgcircle);
                    final RadioButton filled = new RadioButton(MainActivity.this);
                    RadioButton not_filled = new RadioButton(MainActivity.this);
                    filled.setLayoutParams(new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT, 1f));
                    not_filled.setLayoutParams(new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT, 1f));
                    filled.setId(R.id.filled);
                    filled.setText("Filled");
                    not_filled.setId(R.id.not_filled);
                    not_filled.setText("Not Filled");
                    fillColorGroup.addView(filled);
                    fillColorGroup.addView(not_filled);
                    layout.addView(fillColorGroup, dialogLayoutParams);

                    final Button submit = new Button(MainActivity.this);
                    submit.setText(R.string.submit);
                    submit.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    submit.setVisibility(View.VISIBLE);
                    submit.setPadding(30, 30, 30, 30);
                    submit.setId(R.id.submit);
                    layout.addView(submit, dialogLayoutParams);


                    submit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!length.getText().toString().equals("") && !breadth.getText().toString().equals("")) {
                                if (Integer.parseInt(length.getText().toString()) <= 400 && Integer.parseInt(breadth.getText().toString()) <= 600) {
                                    if (fillColorGroup.getCheckedRadioButtonId() == filled.getId()) {
                                        drawRectangle(Integer.parseInt(length.getText().toString()), Integer.parseInt(breadth.getText().toString()), true);
                                    }else {
                                        drawRectangle(Integer.parseInt(length.getText().toString()), Integer.parseInt(breadth.getText().toString()), false);
                                    }
                                    dialog.dismiss();
                                } else {
                                    length.setError("Given sides exceeds the bounds of visible screen. Please give valid sides.");
                                    breadth.setError("Given sides exceeds the bounds of visible screen. Please give valid sides.");

                                }
                            } else if (length.getText().toString().equals("") && !breadth.getText().toString().equals("")) {
                                length.setError("Please enter length");
                            } else if (breadth.getText().toString().equals("") && !length.getText().toString().equals("")) {
                                breadth.setError("Please enter breadth");
                            } else {
                                length.setError("Please enter length");
                                breadth.setError("Please enter breadth");
                            }

                        }
                    });


                } else if (shapesDropdown.getSelectedItem().toString().equals("Square")) {
                    layout.removeAllViewsInLayout();
                    layout.invalidate();
                    layout.addView(shapesDropdown, dialogLayoutParams);
                    dialog.getWindow().setLayout(650, 800);


                    final EditText side = new EditText(MainActivity.this);
                    side.setVisibility(View.VISIBLE);
                    side.setPadding(30, 30, 30, 30);
                    side.setHint("Enter a side");
                    side.setInputType(InputType.TYPE_CLASS_NUMBER);
                    side.requestFocus();
                    side.setId(R.id.side);
                    layout.addView(side, dialogLayoutParams);

                    final RadioGroup fillColorGroup = new RadioGroup(MainActivity.this);
                    fillColorGroup.setLayoutParams(new RadioGroup.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    fillColorGroup.setOrientation(LinearLayout.HORIZONTAL);
                    fillColorGroup.setId(R.id.rgcircle);
                    final RadioButton filled = new RadioButton(MainActivity.this);
                    RadioButton not_filled = new RadioButton(MainActivity.this);
                    filled.setLayoutParams(new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT, 1f));
                    not_filled.setLayoutParams(new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT, 1f));
                    filled.setId(R.id.filled);
                    filled.setText("Filled");
                    not_filled.setId(R.id.not_filled);
                    not_filled.setText("Not Filled");
                    fillColorGroup.addView(filled);
                    fillColorGroup.addView(not_filled);
                    layout.addView(fillColorGroup, dialogLayoutParams);

                    final Button submit = new Button(MainActivity.this);
                    submit.setText(R.string.submit);
                    submit.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    submit.setVisibility(View.VISIBLE);
                    submit.setPadding(30, 30, 30, 30);
                    submit.setId(R.id.submit);
                    layout.addView(submit, dialogLayoutParams);


                    submit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!side.getText().toString().equals("")) {
                                if (Integer.parseInt(side.getText().toString()) <= 420) {
                                    if (fillColorGroup.getCheckedRadioButtonId() == filled.getId()) {
                                        drawSquare(Integer.parseInt(side.getText().toString()), true);
                                    }else {
                                        drawSquare(Integer.parseInt(side.getText().toString()), false);
                                    }
                                    dialog.dismiss();
                                } else {
                                    side.setError("Given side exceeds the bounds of visible screen. Please give a valid side.");
                                }
                            } else
                                side.setError("Please enter a side");
                        }
                    });



                } else if (shapesDropdown.getSelectedItem().toString().equals("Triangle")) {
                    layout.removeAllViewsInLayout();
                    layout.invalidate();
                    layout.addView(shapesDropdown, dialogLayoutParams);
                    dialog.getWindow().setLayout(650, 700);


                    final RadioGroup fillColorGroup = new RadioGroup(MainActivity.this);
                    fillColorGroup.setLayoutParams(new RadioGroup.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    fillColorGroup.setOrientation(LinearLayout.HORIZONTAL);
                    fillColorGroup.setId(R.id.rgcircle);
                    final RadioButton filled = new RadioButton(MainActivity.this);
                    RadioButton not_filled = new RadioButton(MainActivity.this);
                    filled.setLayoutParams(new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT, 1f));
                    not_filled.setLayoutParams(new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT, 1f));
                    filled.setId(R.id.filled);
                    filled.setText("Filled");
                    not_filled.setId(R.id.not_filled);
                    not_filled.setText("Not Filled");
                    fillColorGroup.addView(filled);
                    fillColorGroup.addView(not_filled);
                    layout.addView(fillColorGroup, dialogLayoutParams);

                    final Button submit = new Button(MainActivity.this);
                    submit.setText(R.string.submit);
                    submit.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    submit.setVisibility(View.VISIBLE);
                    submit.setPadding(30, 30, 30, 30);
                    submit.setId(R.id.submit);
                    layout.addView(submit, dialogLayoutParams);


                    submit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (fillColorGroup.getCheckedRadioButtonId() == filled.getId()) {
                                drawTriangle(true);
                            }else {
                                drawTriangle(false);
                            }
                            dialog.dismiss();

                        }
                    });



                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


    }

    private ContentValues contentValues() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        }
        return values;
    }

    private void saveImageToStream(Bitmap bitmap, OutputStream outputStream) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


    public void saveImageToGallery(Bitmap bitmap) {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            ContentValues values = contentValues();
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + getString(R.string.app_name));
            values.put(MediaStore.Images.Media.IS_PENDING, true);

            Uri uri = MainActivity.this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try {
                    saveImageToStream(bitmap, MainActivity.this.getContentResolver().openOutputStream(uri));
                    values.put(MediaStore.Images.Media.IS_PENDING, false);
                    MainActivity.this.getContentResolver().update(uri, values, null, null);
                    isSaved = true;
                    Toast.makeText(context, "Drawing Saved to Gallery Successfully!", Toast.LENGTH_SHORT).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            }
        } else {
            File directory = new File(Environment.getExternalStorageDirectory().toString() + '/' + getString(R.string.app_name));

            if (!directory.exists()) {
                directory.mkdirs();
            }
            String fileName = System.currentTimeMillis() + ".png";
            File file = new File(directory, fileName);
            try {
                saveImageToStream(bitmap, new FileOutputStream(file));
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
                MainActivity.this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                isSaved = true;
                Toast.makeText(context, "Drawing Saved to Gallery Successfully!", Toast.LENGTH_SHORT).show();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onBackPressed() {
        if (!isSaved) {
            new AlertDialog.Builder(context)
                    .setTitle("Save / Exit Wizard")
                    .setMessage("Are you sure you want to exit without saving?")

                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.this.finish();
                        }
                    })

                    .setNegativeButton("No, Save and Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Logic for Saving the Painting
                            verifyStoragePermissions(MainActivity.this);

                            saveImageToGallery(getBitmap(dv));
                            MainActivity.this.finish();

                        }
                    })
                    .show();
        } else {
            new AlertDialog.Builder(context)
                    .setTitle("Exit Wizard")
                    .setMessage("Are you sure you want to exit?")

                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.this.finish();
                        }
                    })

                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {


                        }
                    })
                    .show();
        }
    }
}
