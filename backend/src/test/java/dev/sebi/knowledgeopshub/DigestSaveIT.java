package dev.sebi.knowledgeopshub;

import dev.sebi.knowledgeopshub.jobs.JobsController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DigestSaveIT {

  @Autowired JobsController jobsController;

  @Test
  void save_digest_creates_file() throws Exception {
    var res = jobsController.saveDigest(null, new JobsController.SaveDigestRequest("hola digest"));
    Path p = Path.of(res.path());
    assertTrue(Files.exists(p));
    assertTrue(Files.readString(p).contains("hola digest"));
  }
}
