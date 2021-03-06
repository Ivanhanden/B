package com.handen.bbsms;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.appodeal.ads.Appodeal;
import com.appodeal.ads.BannerCallbacks;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private boolean onCreateCalled = false;

    private static float ostatok = 0;
    final Context context = this;
    static Database database = null;

    private static final int REQUEST_READ_SMS = 10002;

    private PoluchatFragment poluchatFragment;

    static ArrayList<String> smsTextList = new ArrayList<>();
    static ArrayList<Database.SMS> smsList = new ArrayList<>();

    public static ArrayList<String> getSmsTextList() {
        return smsTextList;
    }

    public static ArrayList<Database.SMS> getSmsList(Date date) {
        ArrayList<Database.SMS> ret = new ArrayList<Database.SMS>();
        if (date != null) {
            Date from = (Date) date.clone();
            Date to = (Date) date.clone();
            if (to.getMonth() < 12)
                to = new Date(to.getYear(), to.getMonth() + 1, 1);
            else
                to = new Date(to.getYear() + 1, 1, 1);
            String s = to.toString();
            s = from.toString();
            for (int i = 0; i < smsList.size(); i++) {
                if (smsList.get(i).date != null) {
                    if (from.compareTo(smsList.get(i).date) <= 0 && to.compareTo(smsList.get(i).date) > 0)
                        ret.add(smsList.get(i));
                }
            }
        }
        return ret;
    }

    @Override
    public void onResume() { // "просыпаемся"
        super.onResume();
        if (onCreateCalled) {
            smsList.clear();
            int permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);
            if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                readSMS();
            }
        }
    }

/*
    @Override
    public void onStart() {
    }
*/


    //42201f8561511aed9f538eba26947d43f9788102ca72f3c7
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (database == null)
            database = new Database(this);

        String appKey = "42201f8561511aed9f538eba26947d43f9788102ca72f3c7";
        Appodeal.disableLocationPermissionCheck();
        Appodeal.initialize(this, appKey, Appodeal.BANNER);

        Appodeal.setBannerCallbacks(new BannerCallbacks() {
            @Override
            public void onBannerLoaded(int height, boolean isPrecache) {
                Log.d("Appodeal", "onBannerLoaded");
            }

            @Override
            public void onBannerFailedToLoad() {
                Log.d("Appodeal", "onBannerFailedToLoad");
            }

            @Override
            public void onBannerShown() {
                Log.d("Appodeal", "onBannerShown");
            }

            @Override
            public void onBannerClicked() {
                Log.d("Appodeal", "onBannerClicked");
            }
        });

        Log.d("appodeal", Boolean.toString(Appodeal.isLoaded(Appodeal.BANNER_BOTTOM)));
        Appodeal.show(this, Appodeal.BANNER);

//        if (database == null)
//            database = new Database(this);

        smsList.clear();

        int permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            readSMS();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS},
                    REQUEST_READ_SMS);
        }

        poluchatFragment = PoluchatFragment.newInstance();
        getSupportFragmentManager().beginTransaction().addToBackStack("").replace(R.id.fragmentHost, poluchatFragment, "").commit();
        onCreateCalled = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_SMS:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                    readSMS();
                } else {
                    // permission denied
                    Toast.makeText(this, " Анализ невозможен так как доступ к SMS запрещён.", Toast.LENGTH_LONG).show();
                }
                return;
        }
    }

    private void insert(String id, String date, String body) {
        smsTextList.add(body);
    }

    private void readSMS() {
        Uri inboxURI = Uri.parse("content://sms/inbox");
        String[] reCols = new String[]{"_id", "date", "body"};

        ContentResolver cr = getContentResolver();
        if (cr != null) {
//            Cursor cur = cr.query(inboxURI, reCols, "address LIKE 'ASB.BY'", null, "date");
            Cursor cur = cr.query(inboxURI, reCols, "address = 'ASB.BY' or address = 'ASB'", null, "date");
            if (cur != null) {
                /*
                 * Moves to the next row in the cursor. Before the first movement in the cursor, the
                 * "row pointer" is -1, and if you try to retrieve data at that position you will get an
                 * exception.
                 */
                while (cur.moveToNext()) {
                    insert(cur.getString(0), cur.getString(1), cur.getString(2));
                    Database.SMS sms = new Database.SMS(cur.getString(2));
                    if (sms.result != "OK") {
                        Toast.makeText(this, "SMS parce: " + sms.result, Toast.LENGTH_LONG).show();
                    } else {
                        ostatok = sms.balance;
                        smsList.add(sms);
                    }
                }
            } else {
                // Insert code here to report an error if the cursor is null or the provider threw an exception.
                Toast.makeText(this, "cur == null", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "cr == null", Toast.LENGTH_LONG).show();
        }
        setTitle("Остаток: " + new DecimalFormat("#0.00").format(ostatok) + " BYN");

    }

    @Override
    public void onBackPressed() {
        poluchatFragment.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.item_all:
                poluchatFragment.mode = 3;
                poluchatFragment.updateView();
                break;
            case R.id.item_grouped:
                poluchatFragment.mode = 1;
                poluchatFragment.updateView();
                break;
            case R.id.item_about:
                showAbout();
                break;
            case R.id.item_help:
                showHelp();
                break;
        }

        //noinspection SimplifiableIfStatement
        return super.onOptionsItemSelected(item);
    }

    private void changeFragment(Fragment newFragment, String newFragmentTag) {
        Fragment currentFragment = MainActivity.this.getSupportFragmentManager().findFragmentById(R.id.fragmentHost);
        String currentTag = currentFragment.getTag();

        if (!newFragmentTag.equals(currentFragment.getTag())) {
            getSupportFragmentManager().beginTransaction().addToBackStack(newFragmentTag).replace(R.id.fragmentHost, newFragment, newFragmentTag).commit();
        }
    }

    public void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for N milliseconds
        v.vibrate(20);
    }

    void fragmentRename(final String receiver) {
/*
        final Dialog dialog = new Dialog(MainActivity.this);

        // Установите заголовок
        dialog.setTitle("Заголовок диалога");
        // Передайте ссылку на разметку
        dialog.setContentView(R.layout.dialog_rename_or_comment);
        // Найдите элемент TextView внутри вашей разметки
        // и установите ему соответствующий текст
        String title = "Переименовать получателя \"" + receiver + "\"? Новое имя получателя (пустое поле отменит переименование):";
        TextView text = dialog.findViewById(R.id.dialogTextView);
        text.setText(title);
        EditText newText = dialog.findViewById(R.id.inputText);
        String renames = Database.getRenames(receiver);
        if (renames != null)
            newText.setText(renames);
        else
            newText.setText(receiver);
        int width = (int) (getResources().getDisplayMetrics().widthPixels);
        dialog.findViewById(R.id.buttonCansel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.buttonOk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText newText = dialog.findViewById(R.id.inputText);
                Database.updateRanames(receiver, newText.getText().toString());
                dialog.dismiss();
            }
        });

        dialog.show();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();

        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = width;
        dialog.getWindow().setAttributes(lp);
*/
    }

    void fragmentComment(Database.SMS _sms) {
        final Database.SMS sms = _sms;
        final Dialog dialog = new Dialog(MainActivity.this);

        // Передайте ссылку на разметку
        dialog.setContentView(R.layout.dialog_comment);
        // Найдите элемент TextView внутри вашей разметки
        // и установите ему соответствующий текст
        String title = "Введите комментарий к этому платежу (пустое поле отменит комментарий):";
        TextView text = dialog.findViewById(R.id.dialogTextView);
        text.setText(title);
        EditText newText = dialog.findViewById(R.id.inputText);
        String comment = Database.getComment(sms);
        if (comment != null)
            newText.setText(comment);
        else
            newText.setText("");
        dialog.findViewById(R.id.buttonCansel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.buttonOk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText newText = dialog.findViewById(R.id.inputText);
                Database.updateComment(sms, newText.getText().toString());
                dialog.dismiss();
                poluchatFragment.updateView();
            }
        });

        TableLayout table = dialog.findViewById(R.id.table_comments); // init table
        if (table != null) {
            table.removeAllViews();
            table.invalidate();
            ArrayList<String> comments = Database.getComments();
            for (int i = 0; i < comments.size(); i++) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                TableRow tr = (TableRow) inflater.inflate(R.layout.fragment_row_comment, null);
                TextView tv = tr.findViewById(R.id.col_comment);
                tv.setText(comments.get(i));
                if ((i % 2) == 0)
                    tr.setBackgroundColor(ContextCompat.getColor(this, R.color.color_row1));
                else
                    tr.setBackgroundColor(ContextCompat.getColor(this, R.color.color_row2));
                table.addView(tr); //добавляем созданную строку в таблицу
            }
            if (comments.size() == 0) {
                TextView tv = dialog.findViewById(R.id.dialogTextView2);
                if (tv != null)
                    tv.setText("");
            }
        }

        dialog.show(); //todo Enter обрабатывать как конец ввода

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();

        lp.copyFrom(dialog.getWindow().getAttributes());
        int width = (int) (getResources().getDisplayMetrics().widthPixels);
        lp.width = width;
        dialog.getWindow().setAttributes(lp);

    }

    protected void showAbout() {
        // Inflate the about message contents
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

        // When linking text, force to always use default color. This works
        // around a pressed color state bug.
        TextView textView = (TextView) messageView.findViewById(R.id.about_credits);
//        int defaultColor = textView.getTextColors().getDefaultColor();
//        textView.setTextColor(defaultColor);
//        textView.setTextColor(ContextCompat.getColor(this, R.color.color_rashod));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setIcon(R.drawable.ic_launcher_background);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle(R.string.app_name);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }

    protected void showHelp() {
        // Inflate the about message contents
        View messageView = getLayoutInflater().inflate(R.layout.help, null, false);

//        TextView textView = (TextView) messageView.findViewById(R.id.about_credits);

        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth() / 2; // ((display.getWidth()*20)/100)
        int height = display.getHeight() / 2;// ((display.getHeight()*30)/100)
        TableRow.LayoutParams parms = new TableRow.LayoutParams(width, height);
        ImageView iv = messageView.findViewById(R.id.screenshot1);
        iv.setLayoutParams(parms);
        iv = messageView.findViewById(R.id.screenshot2);
        iv.setLayoutParams(parms);
        iv = messageView.findViewById(R.id.screenshot3);
        iv.setLayoutParams(parms);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle(R.string.help_header);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }

    protected void showPreferences () {
        /*
                                Intent settingsActivity = new Intent (getBaseContext(),
                                                Preferences.class);
                                startActivity (settingsActivity);
                        }
                        
                        private void getPrefs() {
                        boolean CheckboxPreference;
        String ListPreference;
        String editTextPreference;
        String ringtonePreference;
        String secondEditTextPreference;
        String customPref;
                        
                // Get the xml/preferences.xml preferences
                SharedPreferences prefs = PreferenceManager
                                .getDefaultSharedPreferences(getBaseContext());
                CheckboxPreference = prefs.getBoolean("checkboxPref", true);
                ListPreference = prefs.getString("listPref", "nr1");
                editTextPreference = prefs.getString("editTextPref",
                                "Nothing has been entered");
                ringtonePreference = prefs.getString("ringtonePref",
                                "DEFAULT_RINGTONE_URI");
                secondEditTextPreference = prefs.getString("SecondEditTextPref",
                                "Nothing has been entered");
                // Get the custom preference
                SharedPreferences mySharedPreferences = getSharedPreferences(
                                "myCustomSharedPrefs", Activity.MODE_PRIVATE);
                customPref = mySharedPreferences.getString("myCusomPref", "");
                */
        }

}

//todo "translate to "russian""


// TODO вы владелец карты Беларусбанка, вы получаете SMS о операциях по карте, вы хотите узнать куда уходят деньги, но вводить в программе учёта каждый, даже самый мелкий, платёж выше Ваших сил? Это приложение для вас.
// TODO Подарить кофе разработчику. Кофе с шоколадкой. Чай с печеньками.

