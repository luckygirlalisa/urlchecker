import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;

import org.testng.annotations.Test;
import utils.FileFunctions;
import utils.HTTPUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class RedirectTest {

    @Test
    public void testRedirect()
    {

        String urlFilePath = "src/Files/suncorp_test.txt";
        String resultPath = "src/Files/redirect_report_suncorp.csv";

        String resultHeader = "result, response code, original link, destination, actual destination\n";
        FileFunctions.createFile(resultPath,resultHeader);
        try{
            Stream<String> streams = Files.lines(Paths.get(urlFilePath), Charset.defaultCharset());
            streams.forEach(line -> process(line));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void process(String line)
    {
        List<String> parseLine = Arrays.asList(line.split("\t"));
        try{
            URI origLink = new URI(parseLine.get(0));
            URI destination = new URI(parseLine.get(1));

            verifyRedirect(origLink, destination);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void verifyRedirect(URI origLink, URI destination) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException
    {

        try{
            CloseableHttpClient client = HTTPUtils.getClientNoRedirect();
            HttpGet get = new HttpGet(origLink);
            Integer responseCode = client.execute(get).getStatusLine().getStatusCode();

            HttpClientContext context = HttpClientContext.create();
            client = HTTPUtils.getClientForLinkServiceRedirects();
            client.execute(get,context);
            client.close();

            writeToResultFile(origLink, destination, context.getRedirectLocations(), responseCode);
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void writeToResultFile(URI origLink, URI destination, List<URI> redirectLinks, Integer responseCode)
    {
        try{
            Boolean success = HTTPUtils.redirectSuccess(destination, redirectLinks, responseCode);
            FileWriter fileWriter = new FileWriter("src/Files/Redirect_result_bank.csv",true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            String line = success + "," + responseCode + "," + origLink + "," + destination + "," + redirectLinks.toString().replace(",","|") + "\n";
            bufferedWriter.write(line);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
