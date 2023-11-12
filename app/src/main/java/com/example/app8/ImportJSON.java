package com.example.app8;


import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ImportJSON extends AppCompatActivity {
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private void initializeFilePicker() {
        ActivityResultRegistry registry = getActivityResultRegistry();
        filePickerLauncher = registry.register("filePickerKey", new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        // Handle the result here
                        Intent data = result.getData();
                        if(data!=null){
                            Uri uri = data.getData();
                            if(uri!=null){
                                try{
                                    InputStream inputStream = getContentResolver().openInputStream(uri);
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                                    StringBuilder stringBuilder = new StringBuilder();
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        stringBuilder.append(line);
                                    }
                                    reader.close();
                                    inputStream.close();

                                    // Phân tích cú pháp JSON
                                    String jsonString = stringBuilder.toString();
                                    JsonElement jsonElement = JsonParser.parseString(jsonString);

                                    // Truy cập vào các trường dữ liệu
                                    // Ví dụ: Giả sử tệp JSON có trường "name"
                                    if (jsonElement.isJsonObject()) {
                                        String code = jsonElement.getAsJsonObject().get("code").getAsString();
                                        String dateOfBirth = jsonElement.getAsJsonObject().get("code").getAsString();


                                    }
                                }catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                } catch (FileNotFoundException e) {
                                    throw new RuntimeException(e);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }

                    }
                });

        openFilePicker();
    }
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        filePickerLauncher.launch(intent);
    }
}
