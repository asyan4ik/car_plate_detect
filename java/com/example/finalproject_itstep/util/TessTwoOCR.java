package com.example.finalproject_itstep.util;

import android.content.Context;
import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

import static com.googlecode.tesseract.android.TessBaseAPI.PageSegMode.PSM_AUTO;

public final class TessTwoOCR {

    private static TessBaseAPI tessBaseAPI = new TessBaseAPI();
    Context context;
    private String tessTrainDataPath;

    public TessTwoOCR(Context context) {
        this.context = context;
        tessTrainDataPath = context.getExternalFilesDir("/").getPath() + "/";
        tessBaseAPI.setDebug(true);

        tessBaseAPI.init(tessTrainDataPath, "eng");
        tessBaseAPI.setPageSegMode(PSM_AUTO);
    }

    public String getOCRResult(Bitmap bitmap) {

        String charList = "ABCEHIKMOPTUXY0123456789-";
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, charList);
        tessBaseAPI.setImage(bitmap);
        String result = tessBaseAPI.getUTF8Text();
        return result;
    }

    public void stopRecognition() {
        tessBaseAPI.stop();
    }

    public void onDestroy() {
        if (tessBaseAPI != null)
            tessBaseAPI.end();
    }
}
