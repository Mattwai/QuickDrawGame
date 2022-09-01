package nz.ac.auckland.se206.words;

import static org.junit.jupiter.api.Assertions.*;

import com.opencsv.exceptions.CsvException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategorySelectorTest {
  @Test
  public void testCSVreader() throws IOException, URISyntaxException, CsvException {
    CategorySelector category = new CategorySelector();
    List<String[]> result = category.getLines();
    int size = result.size();
    assertTrue(size == 345);
  }
}
