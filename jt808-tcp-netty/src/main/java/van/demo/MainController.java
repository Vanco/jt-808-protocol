package van.demo;

import cn.hylexus.jt808.service.codec.MsgDecoder;
import cn.hylexus.jt808.util.HexStringUtils;
import cn.hylexus.jt808.vo.PackageData;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * &copy; fanhuagang@gmail.com
 * Created by van on 2018/4/11.
 */
public class MainController implements Initializable {
    public TextArea logger;
    private MsgDecoder decoder = new MsgDecoder();

    public TextArea target;
    public TextArea input;

    public void decode(ActionEvent actionEvent) {

        String text = input.getText();

        if (text.trim().length() == 0) {
            logger.appendText("Please Input...\n");
            return;
        }

        String base64Patten = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
        if (Pattern.compile(base64Patten).matcher(text).matches()) {
            text = new String(Base64.getDecoder().decode(text.getBytes()));
            logger.appendText("Decoded Base64:");
            logger.appendText(text);
            logger.appendText("\n");
        }

        PackageData packageData = decoder.bytes2PackageData(HexStringUtils.hexString2Bytes(text.replaceFirst("^7e", "").replaceFirst("7e$", "")).getBytes());
        target.setText(packageData.toString());
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
