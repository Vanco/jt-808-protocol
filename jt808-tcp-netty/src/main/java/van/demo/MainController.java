package van.demo;

import cn.hylexus.jt808.service.codec.MsgDecoder;
import cn.hylexus.jt808.util.HexStringUtils;
import cn.hylexus.jt808.vo.PackageData;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;

import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * &copy; fanhuagang@gmail.com
 * Created by van on 2018/4/11.
 */
public class MainController implements Initializable {
    public TextArea logger;
    public RadioButton rbHex;
    public RadioButton rbBase64;
    private MsgDecoder decoder = new MsgDecoder();

    public TextArea target;
    public TextArea input;

    public void decode(ActionEvent actionEvent) {

        String text = input.getText().trim();

        if (text.trim().length() == 0) {
            logger.appendText("Please Input...\n");
            return;
        }

        if (rbBase64.isSelected()) {
            byte[] bs = Base64.getDecoder().decode(text.getBytes());
            byte[] bytes = Arrays.copyOfRange(bs, 1, bs.length - 1);
            text = HexStringUtils.toHexString(bytes);
            logger.appendText("Decoded Base64:");
            logger.appendText(text);
            logger.appendText("\n");

            PackageData packageData = decoder.bytes2PackageData(bytes);
            target.appendText(packageData.getMsgHeader().toString());
            target.appendText("\n");
            target.appendText(decoder.toBodyString(packageData));
        } else {
            String hex = text.replaceFirst("^7[e|E]", "").replaceFirst("7[e|E]$", "");

            PackageData packageData = decoder.bytes2PackageData(HexStringUtils.hexString2Bytes(hex).getBytes());
            target.appendText(packageData.getMsgHeader().toString());
            target.appendText("\n");
            target.appendText(decoder.toBodyString(packageData));
        }

        input.requestFocus();
    }

    public void reset(ActionEvent actionEvent) {
        input.clear();
        target.clear();
        input.requestFocus();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TextAreaAppender.setTextArea(logger);
    }

    public void clearLog(ActionEvent actionEvent) {
        logger.clear();
        input.requestFocus();
    }
}
