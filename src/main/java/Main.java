import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class Main {

//    PUT .xlsx FILE WITH PROMPTS IN RESOURCES AND INDICATE IT'S NAME IN RUN ARGS
    //    GENERATE prompts.ts FILE FROM .xlsx FILE
    public static void main(String[] args) throws IOException {
        String xlsxFromFileName = args[0];

        InputStream resourceAsStream = Main.class.getResourceAsStream(xlsxFromFileName);
        Workbook workbook = new XSSFWorkbook(Objects.requireNonNull(resourceAsStream));
        Sheet sheet = workbook.getSheetAt(0);
        StringBuilder appendOutputBuilder = new StringBuilder();
        appendOutputBuilder.append("const prompts = {")
                .append("\n");

//        SECTION FOR BUILDING PROMPTS AS JSON FOR .ts FILE
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row.getCell(2) != null && row.getCell(4) != null && row.getCell(5) != null) {
                String cell2 = row.getCell(2)
                        .toString()
                        .replaceAll("'", "\\\\'")
                        .trim();

                String cell4 = row.getCell(4)
                        .toString()
                        .replaceAll("(['«»])", "¿")
                        .trim();
                int phase = 1;
                while (cell4.contains("¿")) {
                    if (phase % 2 == 0) {
                        cell4 = cell4.replaceFirst("¿", "»");
                    } else {
                        cell4 = cell4.replaceFirst("¿", "«");
                    }
                    phase++;
                }

                String cell5 = row.getCell(5)
                        .toString()
                        .replaceAll("(\n| \n)", "\\\\n' +\n'")
                        .replaceAll("Don't", "Do not")
                        .replaceAll("'s", "\\\\'s")
                        .replaceAll("[ ]{2,}", "\\\\n' +\n'")
                        .trim();

                StringBuilder promptBuilder = new StringBuilder();

                promptBuilder.append("'")
                        .append(cell2)
                        .append("': {")
                        .append("\n")
                        .append("system: '")
                        .append(cell4)
                        .append("',")
                        .append("\n")
                        .append("user: '")
                        .append(cell5)
                        .append("},")
                        .append("\n");

                if (promptBuilder.substring(promptBuilder.length() - 7, promptBuilder.length() - 3).equals(" +\n'")) {
                    promptBuilder.replace(promptBuilder.length() - 7, promptBuilder.length() - 3, "");
                }
                if (promptBuilder.substring(promptBuilder.length() - 4).equals(":},\n")) {
                    promptBuilder.replace(promptBuilder.length() - 4, promptBuilder.length(), ":\\\\n'\n},\n");
                }

                appendOutputBuilder.append(promptBuilder);
            }
        }
        appendOutputBuilder
                .append('}');
        String appendOutputString = appendOutputBuilder.toString();

        OutputStreamWriter outputStreamWriter =
                new OutputStreamWriter(
                        Files.newOutputStream(Path.of("src/main/resources/prompts.ts")));
        outputStreamWriter.append(appendOutputString);
        outputStreamWriter.flush();
        outputStreamWriter.close();

    }
}

