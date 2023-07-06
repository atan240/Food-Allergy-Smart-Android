package com.example.foodallergysmart;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ImageViewCompat;

import android.app.Activity;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
{
    Button btn_scan;
    ImageView mimageViewItem;
    private TextView mtxtItemName;
    private TextView mTextViewResult;
    private String productImage;
    private String barcodeValue;
    private String productName;
    private String productAllergens;
    private String productTraces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mimageViewItem = findViewById(R.id.imageViewItem);
        mTextViewResult = findViewById(R.id.text_view_result);
        mtxtItemName = findViewById(R.id.txtItemName);

        btn_scan = findViewById(R.id.btn_scan);
        btn_scan.setOnClickListener(view -> {
            Glide.with(MainActivity.this).load("").into(mimageViewItem);
            scanCode();
        });
    }

    private void scanCode()
    {
        ScanOptions options= new ScanOptions();
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }

    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result ->
    {
       if(result.getContents() !=null)
       {
           barcodeValue = result.getContents();
           createRequest(barcodeValue);
       }
    });

//    GET request querying OpenFoodFacts API using barcode from scanning
    private void createRequest(String barcodeValue) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://world.openfoodfacts.org/api/v0/product/" + barcodeValue + ".json";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        JSONObject productObject = jsonResponse.getJSONObject("product");
//                        productImage = productObject.getString("code");
                        productImage = productObject.getString("image_url");
                        productName = productObject.getString("product_name");
                        productAllergens = productObject.getString("allergens");
                        productTraces = productObject.getString("traces");

                        // Code for taking string and splitting into an array of Allergens
                        String[] allergensArray = productAllergens.split("(,|en:)");
                        List<String> resultList = new ArrayList<>();
                        for (String element : allergensArray) {
                            String trimmedElement = element.trim();
                            if (!trimmedElement.isEmpty()) {
                                resultList.add(trimmedElement);
                            }
                        }
                        String[] resultArray = resultList.toArray(new String[0]);

                        StringBuilder allergensBuilder = new StringBuilder();
                        for (String allergen : resultArray) {
                            allergensBuilder.append(allergen).append(", ");
                        }
                        String allergensText = allergensBuilder.toString();
                        if (allergensText.endsWith(", ")) {
                            allergensText = allergensText.substring(0, allergensText.length() - 2);
                        }

                        final String finalAllergensText = allergensText;

                        // Code for taking string and splitting into an array of "Traces of" ingredients
                        String[] TracesArray = productTraces.split("(,|en:)");

                        List<String> tresultList = new ArrayList<>();
                        for (String element : TracesArray) {
                            String trimmedElement = element.trim();
                            if (!trimmedElement.isEmpty()) {
                                tresultList.add(trimmedElement);
                            }
                        }
                        String[] tresultArray = tresultList.toArray(new String[0]);

                        StringBuilder TracesBuilder = new StringBuilder();
                        for (String traces : tresultArray) {
                            TracesBuilder.append(traces).append(", ");
                        }
                        String TracesText = TracesBuilder.toString();
                        if (TracesText.endsWith(", ")) {
                            TracesText = TracesText.substring(0, TracesText.length() - 2);
                        }

                        final String finalTracesText = TracesText;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                Show error message if no results available in database
                                if (finalAllergensText.isEmpty()) {
                                    mtxtItemName.setText(productName.toString());
                                    mTextViewResult.setText(new StringBuilder()
                                            .append("Error: No results found. Please refer to item packaging for allergens")
                                            .append(finalAllergensText)
                                            .toString());
                                }

//                                Show allergen result and "traces of" result
                                else if (!finalTracesText.isEmpty()) {
                                    Glide.with(MainActivity.this).load(productImage).into(mimageViewItem);
                                    mtxtItemName.setText(productName.toString());
                                    mTextViewResult.setText(new StringBuilder()
                                            .append(" Contains the following allergens: ")
                                            .append(finalAllergensText)
                                            .append("\n")
                                            .append("\n")
                                            .append(" Also contains traces of: ")
                                            .append(finalTracesText)
                                            .toString());
                                }
//                                If no "traces of" in ingredients list, then show just allergen results
                                else {
                                    Glide.with(MainActivity.this).load(productImage).into(mimageViewItem);
                                    mtxtItemName.setText(productName.toString());
                                    mTextViewResult.setText(new StringBuilder()
                                            .append(" Contains the following allergens: ")
                                            .append(finalAllergensText)
                                            .toString());
                                }
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
    }

}