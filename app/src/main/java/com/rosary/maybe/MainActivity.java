package com.rosary.maybe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private SharedPreferences prefs;

    private int count = 0;
    private int goal = 33;
    private int dhikrIndex = 0;

    private final int background = Color.rgb(8,17,15);
    private final int cardColor = Color.rgb(19,36,29);
    private final int green = Color.rgb(32,180,105);
    private final int lightGreen = Color.rgb(178,255,208);
    private final int white = Color.WHITE;
    private final int gray = Color.rgb(174,193,181);

    private final String[] adhkar = {
        "سبحان الله",
        "الحمد لله",
        "الله أكبر",
        "أستغفر الله",
        "لا إله إلا الله",
        "سبحان الله وبحمده",
        "لا حول ولا قوة إلا بالله",
        "اللهم صلِّ وسلم على نبينا محمد"
    };

    private final int[] goals = {33, 100, 1000, 0};

    private TextView counter, dhikrText, progressText;
    private Spinner dhikrSpinner, goalSpinner;

    private MediaPlayer player;

    private final ArrayList<String> reciterNames = new ArrayList<>();
    private final ArrayList<String> reciterServers = new ArrayList<>();
    private final ArrayList<String> reciterSurahLists = new ArrayList<>();

    private final ArrayList<String> surahNames = new ArrayList<>();
    private final ArrayList<Integer> surahNumbers = new ArrayList<>();

    private Spinner reciterSpinner, surahSpinner;
    private Button playButton;

    private int selectedReciter = 0;
    private int selectedSurah = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("misbaha_data", MODE_PRIVATE);

        count = Math.max(0, prefs.getInt("count", 0));
        goal = prefs.getInt("goal", 33);
        dhikrIndex = prefs.getInt("dhikr", 0);

        if (goal != 33 && goal != 100 && goal != 1000 && goal != 0) {
            goal = 33;
        }

        if (dhikrIndex < 0 || dhikrIndex >= adhkar.length) {
            dhikrIndex = 0;
        }

        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);

        showHome();
    }

    private void showHome() {
        LinearLayout root = createRoot();

        root.addView(makeText("📿 المسبحة الإلكترونية", 27, white, true),
                fullWidth());

        root.addView(makeText("سبّح واطمئن قلبك", 16, lightGreen, false),
                withTop(10));

        Button quranButton = makeButton("📖 القرآن الكريم والتلاوات", green);
        root.addView(quranButton, withTop(20));
        quranButton.setOnClickListener(v -> showQuran());

        Button hadithButton = makeButton("📚 الأحاديث النبوية", cardColor);
        root.addView(hadithButton, withTop(10));
        hadithButton.setOnClickListener(v -> showHadiths());

        LinearLayout dhikrCard = makeCard();

        TextView chooseLabel = makeText("اختر الذكر", 16, gray, true);
        chooseLabel.setGravity(Gravity.CENTER);
        dhikrCard.addView(chooseLabel, fullWidth());

        dhikrSpinner = new Spinner(this);

        ArrayAdapter<String> dhikrAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, adhkar);

        dhikrAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        dhikrSpinner.setAdapter(dhikrAdapter);
        dhikrSpinner.setSelection(dhikrIndex);

        dhikrCard.addView(dhikrSpinner, withTop(10));
        root.addView(dhikrCard, withTop(18));

        dhikrSpinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view, int position, long id) {
                        dhikrIndex = position;
                        saveData();
                        updateDisplay();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {}
                });

        LinearLayout counterCard = makeCard();
        counterCard.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView label = makeText("عدد التسبيحات", 17, gray, true);
        label.setGravity(Gravity.CENTER);
        counterCard.addView(label, fullWidth());

        counter = makeText("000", 60, lightGreen, true);
        counter.setGravity(Gravity.CENTER);
        counterCard.addView(counter, withTop(8));

        dhikrText = makeText(adhkar[dhikrIndex], 21, white, true);
        dhikrText.setGravity(Gravity.CENTER);
        counterCard.addView(dhikrText, withTop(5));

        progressText = makeText("", 14, lightGreen, false);
        progressText.setGravity(Gravity.CENTER);
        counterCard.addView(progressText, withTop(12));

        Button tapButton = makeButton("سَبِّح", green);
        tapButton.setTextSize(27);

        LinearLayout.LayoutParams tapParams =
                new LinearLayout.LayoutParams(dp(215), dp(130));

        tapParams.gravity = Gravity.CENTER;
        tapParams.topMargin = dp(15);

        counterCard.addView(tapButton, tapParams);
        tapButton.setOnClickListener(v -> addTasbeeh());

        root.addView(counterCard, withTop(18));

        LinearLayout settings = makeCard();

        TextView goalLabel = makeText("🎯 هدف التسبيح", 17, white, true);
        goalLabel.setGravity(Gravity.CENTER);
        settings.addView(goalLabel, fullWidth());

        String[] goalNames = {
                "33 تسبيحة", "100 تسبيحة",
                "1000 تسبيحة", "بدون هدف"
        };

        goalSpinner = new Spinner(this);

        ArrayAdapter<String> goalAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, goalNames);

        goalAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        goalSpinner.setAdapter(goalAdapter);
        goalSpinner.setSelection(getGoalIndex());

        settings.addView(goalSpinner, withTop(10));

        goalSpinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view, int position, long id) {
                        goal = goals[position];
                        saveData();
                        updateDisplay();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {}
                });

        Button resetButton = makeButton(
                "تصفير العداد", Color.rgb(120,45,45));

        settings.addView(resetButton, withTop(12));
        resetButton.setOnClickListener(v -> confirmReset());

        root.addView(settings, withTop(18));

        TextView footer = makeText(
                "يتم حفظ عدد التسبيحات • التلاوات تحتاج إنترنت",
                12, gray, false);

        footer.setGravity(Gravity.CENTER);
        root.addView(footer, withTop(15));

        setContentView(root);
        updateDisplay();
    }

    private void showQuran() {
        stopAudio();

        LinearLayout root = createRoot();

        Button back = makeButton("← الرجوع للمسبحة", cardColor);
        root.addView(back, fullWidth());
        back.setOnClickListener(v -> showHome());

        TextView title = makeText(
                "📖 القرآن الكريم والتلاوات", 24, lightGreen, true);

        title.setGravity(Gravity.CENTER);
        root.addView(title, withTop(18));

        TextView info = makeText(
                "اختر القارئ والسورة ثم اضغط تشغيل",
                15, gray, false);

        info.setGravity(Gravity.CENTER);
        root.addView(info, withTop(8));

        root.addView(makeText("🎙️ اختر القارئ", 17, white, true),
                withTop(20));

        reciterSpinner = new Spinner(this);
        root.addView(reciterSpinner, withTop(8));

        root.addView(makeText("📚 اختر السورة", 17, white, true),
                withTop(18));

        surahSpinner = new Spinner(this);
        root.addView(surahSpinner, withTop(8));

        playButton = makeButton("▶ تشغيل التلاوة", green);
        root.addView(playButton, withTop(18));

        playButton.setOnClickListener(v -> playSelectedSurah());

        Button stopButton = makeButton("■ إيقاف التلاوة", cardColor);
        root.addView(stopButton, withTop(8));
        stopButton.setOnClickListener(v -> stopAudio());

        TextView source = makeText(
                "مصدر بيانات القرّاء والتلاوات: MP3Quran",
                12, gray, false);

        root.addView(source, withTop(20));

        setContentView(root);

        loadSurahList();
        loadReciters();
    }

    private void loadSurahList() {
        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(
                        "https://www.mp3quran.net/api/v3/suwar?language=ar");

                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);

                JSONArray array = new JSONArray(
                        readResponse(connection));

                ArrayList<String> names = new ArrayList<>();
                ArrayList<Integer> numbers = new ArrayList<>();

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);

                    String name = obj.optString(
                            "name", obj.optString("sura_name_ar", ""));

                    int number = obj.optInt("id", i + 1);

                    if (!name.isEmpty()) {
                        names.add((i + 1) + ". " + name);
                        numbers.add(number);
                    }
                }

                runOnUiThread(() -> {
                    surahNames.clear();
                    surahNumbers.clear();

                    surahNames.addAll(names);
                    surahNumbers.addAll(numbers);

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            surahNames);

                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item);

                    surahSpinner.setAdapter(adapter);
                });

            } catch (Exception e) {
                runOnUiThread(() -> showMessage(
                        "تعذّر تحميل أسماء السور. تأكد من الإنترنت وحاول مجددًا."));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void loadReciters() {
        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(
                        "https://www.mp3quran.net/api/v3/reciters?language=ar");

                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);

                JSONObject response = new JSONObject(
                        readResponse(connection));

                JSONArray array = response.getJSONArray("reciters");

                ArrayList<String> names = new ArrayList<>();
                ArrayList<String> servers = new ArrayList<>();
                ArrayList<String> lists = new ArrayList<>();

                for (int i = 0; i < array.length(); i++) {
                    JSONObject reciter = array.getJSONObject(i);
                    JSONArray moshaf = reciter.optJSONArray("moshaf");

                    if (moshaf == null || moshaf.length() == 0) continue;

                    for (int j = 0; j < moshaf.length(); j++) {
                        JSONObject edition = moshaf.getJSONObject(j);

                        String server = edition.optString("server", "");
                        String surahList = edition.optString("surah_list", "");

                        if (!server.isEmpty() && !surahList.isEmpty()) {
                            names.add(reciter.optString("name", "قارئ")
                                    + " — " + edition.optString("name", "رواية"));

                            servers.add(server);
                            lists.add(surahList);
                        }
                    }
                }

                runOnUiThread(() -> {
                    reciterNames.clear();
                    reciterServers.clear();
                    reciterSurahLists.clear();

                    reciterNames.addAll(names);
                    reciterServers.addAll(servers);
                    reciterSurahLists.addAll(lists);

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            reciterNames);

                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item);

                    reciterSpinner.setAdapter(adapter);
                });

            } catch (Exception e) {
                runOnUiThread(() -> showMessage(
                        "تعذّر تحميل القرّاء. تحقق من الإنترنت."));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private String readResponse(HttpURLConnection connection)
            throws Exception {

        java.io.InputStream input = connection.getInputStream();
        java.io.ByteArrayOutputStream output =
                new java.io.ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int length;

        while ((length = input.read(buffer)) != -1) {
            output.write(buffer, 0, length);
        }

        input.close();

        return output.toString("UTF-8");
    }

    private void playSelectedSurah() {
        if (reciterServers.isEmpty() || surahNumbers.isEmpty()) {
            showMessage("انتظر حتى يكتمل تحميل القرّاء والسور.");
            return;
        }

        int reciterPosition = reciterSpinner.getSelectedItemPosition();
        int surahPosition = surahSpinner.getSelectedItemPosition();

        if (reciterPosition < 0 || surahPosition < 0) return;

        int surahNumber = surahNumbers.get(surahPosition);

        String allowedSurahs = reciterSurahLists.get(reciterPosition);
        boolean available = false;

        for (String value : allowedSurahs.split(",")) {
            try {
                if (Integer.parseInt(value.trim()) == surahNumber) {
                    available = true;
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (!available) {
            showMessage("هذه السورة غير متوفرة ضمن الرواية المختارة لهذا القارئ.");
            return;
        }

        String fileName = String.format(
                Locale.US, "%03d.mp3", surahNumber);

        String audioUrl = reciterServers.get(reciterPosition) + fileName;

        stopAudio();

        playButton.setText("⏳ جارٍ تجهيز التلاوة...");

        player = new MediaPlayer();

        try {
            player.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());

            player.setDataSource(audioUrl);

            player.setOnPreparedListener(mp -> {
                mp.start();
                playButton.setText("▶ التلاوة تعمل");
            });

            player.setOnCompletionListener(mp -> {
                playButton.setText("▶ تشغيل التلاوة");
                stopAudio();
            });

            player.setOnErrorListener((mp, what, extra) -> {
                playButton.setText("▶ تشغيل التلاوة");
                stopAudio();

                showMessage(
                        "تعذّر تشغيل الصوت. جرّب قارئًا آخر أو تحقق من الإنترنت.");

                return true;
            });

            player.prepareAsync();

        } catch (Exception e) {
            stopAudio();
            playButton.setText("▶ تشغيل التلاوة");
            showMessage("حدث خطأ أثناء تشغيل التلاوة.");
        }
    }

    private void stopAudio() {
        if (player != null) {
            try {
                if (player.isPlaying()) player.stop();
            } catch (Exception ignored) {}

            try {
                player.reset();
                player.release();
            } catch (Exception ignored) {}

            player = null;
        }

        if (playButton != null) {
            playButton.setText("▶ تشغيل التلاوة");
        }
    }

    private void showHadiths() {
        LinearLayout root = createRoot();

        Button back = makeButton("← الرجوع للمسبحة", cardColor);
        root.addView(back, fullWidth());
        back.setOnClickListener(v -> showHome());

        TextView title = makeText(
                "📚 أحاديث نبوية", 25, lightGreen, true);

        title.setGravity(Gravity.CENTER);
        root.addView(title, withTop(18));

        addHadith(root,
                "إنما الأعمال بالنيات",
                "الراوي: عمر بن الخطاب رضي الله عنه",
                "المصدر: صحيح البخاري، وصحيح مسلم.");

        addHadith(root,
                "الدين النصيحة",
                "الراوي: تميم الداري رضي الله عنه",
                "المصدر: صحيح مسلم.");

        addHadith(root,
                "المسلم من سلم المسلمون من لسانه ويده",
                "الراوي: عبد الله بن عمرو رضي الله عنهما",
                "المصدر: صحيح البخاري.");

        addHadith(root,
                "لا يؤمن أحدكم حتى يحب لأخيه ما يحب لنفسه",
                "الراوي: أنس بن مالك رضي الله عنه",
                "المصدر: صحيح البخاري، وصحيح مسلم.");

        addHadith(root,
                "من كان يؤمن بالله واليوم الآخر فليقل خيرًا أو ليصمت",
                "الراوي: أبو هريرة رضي الله عنه",
                "المصدر: صحيح البخاري، وصحيح مسلم.");

        addHadith(root,
                "يسروا ولا تعسروا، وبشروا ولا تنفروا",
                "الراوي: أنس بن مالك رضي الله عنه",
                "المصدر: صحيح البخاري، وصحيح مسلم.");

        addHadith(root,
                "الكلمة الطيبة صدقة",
                "الراوي: أبو هريرة رضي الله عنه",
                "المصدر: صحيح البخاري، وصحيح مسلم.");

        addHadith(root,
                "من لا يرحم لا يُرحم",
                "الراوي: جرير بن عبد الله رضي الله عنه",
                "المصدر: صحيح البخاري، وصحيح مسلم.");

        addHadith(root,
                "الطهور شطر الإيمان",
                "الراوي: أبو مالك الأشعري رضي الله عنه",
                "المصدر: صحيح مسلم.");

        addHadith(root,
                "اتق الله حيثما كنت، وأتبع السيئة الحسنة تمحها، وخالق الناس بخلق حسن",
                "الراوي: أبو ذر رضي الله عنه",
                "المصدر: جامع الترمذي؛ حسّنه الترمذي.");

        TextView note = makeText(
                "هذه مجموعة أولية من الأحاديث. يلزم توسيع قاعدة البيانات من مصدر موثوق لإضافة مجموعات كبيرة.",
                13, gray, false);

        root.addView(note, withTop(15));

        setContentView(root);
    }

    private void addHadith(
            LinearLayout root, String text, String narrator, String source) {

        LinearLayout card = makeCard();

        TextView hadithText = makeText(text, 19, lightGreen, true);
        hadithText.setGravity(Gravity.RIGHT);
        card.addView(hadithText, fullWidth());

        TextView narratorText = makeText(narrator, 14, white, false);
        narratorText.setGravity(Gravity.RIGHT);
        card.addView(narratorText, withTop(10));

        TextView sourceText = makeText(source, 13, gray, false);
        sourceText.setGravity(Gravity.RIGHT);
        card.addView(sourceText, withTop(5));

        Button copy = makeButton("نسخ الحديث", green);
        card.addView(copy, withTop(10));

        copy.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager)
                            getSystemService(CLIPBOARD_SERVICE);

            android.content.ClipData clip =
                    android.content.ClipData.newPlainText(
                            "حديث نبوي", text + "\n" + narrator + "\n" + source);

            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, "تم نسخ الحديث", Toast.LENGTH_SHORT).show();
        });

        root.addView(card, withTop(12));
    }

    private void addTasbeeh() {
        if (count < Integer.MAX_VALUE) count++;

        saveData();
        updateDisplay();

        if (goal > 0 && count == goal) {
            new AlertDialog.Builder(this)
                    .setTitle("ما شاء الله!")
                    .setMessage("وصلت إلى هدفك: " + goal + " تسبيحة.")
                    .setPositiveButton("متابعة", null)
                    .setNegativeButton("تصفير العداد", (dialog, which) -> {
                        count = 0;
                        saveData();
                        updateDisplay();
                    })
                    .show();
        }
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("تصفير العداد؟")
                .setMessage("هل أنت متأكد أنك تريد تصفير عدد التسبيحات؟")
                .setPositiveButton("نعم، صفّر", (dialog, which) -> {
                    count = 0;
                    saveData();
                    updateDisplay();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void updateDisplay() {
        if (counter == null) return;

        counter.setText(String.format(
                Locale.getDefault(), "%03d", count));

        dhikrText.setText(adhkar[dhikrIndex]);

        if (goal == 0) {
            progressText.setText("التسبيح بدون هدف محدد");
        } else if (count >= goal) {
            progressText.setText("اكتمل الهدف • يمكنك المتابعة");
        } else {
            progressText.setText(
                    "باقي " + (goal - count) + " للوصول إلى الهدف");
        }
    }

    private void saveData() {
        if (prefs == null) return;

        prefs.edit()
                .putInt("count", count)
                .putInt("goal", goal)
                .putInt("dhikr", dhikrIndex)
                .apply();
    }

    private int getGoalIndex() {
        for (int i = 0; i < goals.length; i++) {
            if (goals[i] == goal) return i;
        }
        return 0;
    }

    private LinearLayout createRoot() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(background);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(25));

        scroll.addView(root);
        setContentView(scroll);

        return root;
    }

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(17), dp(16), dp(17));
        card.setBackground(rounded(cardColor, 22));
        card.setElevation(dp(3));
        return card;
    }

    private TextView makeText(
            String value, int size, int color, boolean bold) {

        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);

        if (bold) {
            text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return text;
    }

    private Button makeButton(String value, int color) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(15);
        button.setTextColor(white);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(rounded(color, 15));
        return button;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams withTop(int top) {
        LinearLayout.LayoutParams params = fullWidth();
        params.topMargin = dp(top);
        return params;
    }

    private int dp(float value) {
        return (int) (
                value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveData();
    }

    @Override
    protected void onDestroy() {
        stopAudio();
        super.onDestroy();
    }
}