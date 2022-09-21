import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class NasaImages {

    public static CloseableHttpClient httpClient = HttpClients.createDefault();
    public static final String URL =
            "https://api.nasa.gov/planetary/apod?api_key=ekqFTOhMp6tbUAUhWBXwmAEk0nxMl2akaf1kdveI";
    public static String photoUrl = null;
    public static File file = null;

    public static void main(String[] args) {
        String date = getDateFromUser();
        getEntityByDate(date);
        if (photoUrl != null) savingPhotoByUrl(photoUrl, file);

        try {
            httpClient.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    // получение URL фото на заданную дату с сайта NASA
    public static void getEntityByDate(String date) {
        try (CloseableHttpResponse response = httpClient.execute(
                new HttpGet(URL + "&date=" + date))) {
            System.out.println("Requesting information from NASA..");
            String nasaEntity = EntityUtils.toString(response.getEntity());
            parseEntity(nasaEntity);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    // парсинг json сущности от NASA
    public static void parseEntity(String entity) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String mediaType = mapper.readTree(entity).get("media_type").textValue();
            photoUrl = mapper.readTree(entity).get("url").textValue();

            // если тип сущности видео, то вытаскиваем картинку с превью по другому URL
            if ("video".equals(mediaType)) {
                photoUrl = "https://img.youtube.com/vi" + getPreviewId(photoUrl) + "/maxresdefault.jpg";
                String title = mapper.readTree(entity).get("title").textValue();
                file = new File(title + ".jpg");
            } else {
                file = new File(getFileName(photoUrl));
            }
        } catch (NullPointerException e) {
            String msg = mapper.readTree(entity).get("msg").textValue();
            System.out.println(msg);
        }
    }

    // получение и сохранение фото по URL
    public static void savingPhotoByUrl(String url, File file) {
        try (CloseableHttpResponse responsePhoto = httpClient.execute(new HttpGet(url))) {
            System.out.println("Taking photo..");
            byte[] photo = responsePhoto.getEntity().getContent().readAllBytes();
            if (file.exists()) {
                System.out.println("Photo " + file + " already exists");
                return;
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(photo);
            System.out.println("Photo " + file + " successfully saved");
            fos.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static String getFileName(String url) {
        return url
                .substring(url.lastIndexOf('/'))
                .replaceFirst("/", "");
    }

    public static String getPreviewId(String url) {
        return url.substring(url.lastIndexOf('/'), url.indexOf('?'));
    }

    public static String getDateFromUser() {
        String date;
        do {
            System.out.println("Please enter the date by format YYYY-MM-DD:");
            Scanner scan = new Scanner(System.in);
            date = scan.nextLine();
        } while (!date.matches("\\d{4}-\\d{2}-\\d{2}"));
        return date;
    }
}
