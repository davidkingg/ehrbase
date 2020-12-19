package org.ehrbase.dao.access.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.nedap.archie.rm.directory.Folder;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.test_data.folder.FolderTestDataCanonicalJson;
import org.junit.Test;

public class FolderUtilsTest {

  CanonicalJson canonicalJson = new CanonicalJson();

  @Test(expected = IllegalArgumentException.class)
  public void detectsDuplicateFolderNames() throws IOException {

    String value =
        IOUtils.toString(
            FolderTestDataCanonicalJson.FOLDER_WITH_DUPLICATE_NAMES.getStream(), UTF_8);
    Folder testFolder = canonicalJson.unmarshal(value, Folder.class);

    FolderUtils.checkSiblingNameConflicts(testFolder);
  }

  @Test
  public void acceptsFoldersWithoutConflicts() throws IOException {

    String value =
        IOUtils.toString(
            FolderTestDataCanonicalJson.FOLDER_WITHOUT_DUPLICATE_NAMES.getStream(), UTF_8);
    Folder testFolder = canonicalJson.unmarshal(value, Folder.class);

    FolderUtils.checkSiblingNameConflicts(testFolder);
  }
}
