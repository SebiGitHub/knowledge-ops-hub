package dev.sebi.knowledgeopshub.documents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;

@Service
public class StorageService {

  private final Path baseDir;

  public StorageService(@Value("${app.storage.dir}") String dir) {
    this.baseDir = Paths.get(dir).toAbsolutePath().normalize();
  }

  public Path ensureBaseDir() throws IOException {
    Files.createDirectories(baseDir);
    return baseDir;
  }

  public Path store(byte[] bytes, String relativePath) throws IOException {
    ensureBaseDir();
    Path target = baseDir.resolve(relativePath).normalize();
    if (!target.startsWith(baseDir)) {
      throw new IllegalArgumentException("Ruta inválida");
    }
    Files.createDirectories(target.getParent());
    return Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }

  public Path storeFromLocalPath(String localPath, String relativePath) throws IOException {
    ensureBaseDir();
    Path source = Paths.get(localPath).toAbsolutePath().normalize();
    Path target = baseDir.resolve(relativePath).normalize();
    if (!target.startsWith(baseDir)) throw new IllegalArgumentException("Ruta inválida");
    Files.createDirectories(target.getParent());
    return Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
  }
}
