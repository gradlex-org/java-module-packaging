/*
 * Copyright the GradleX team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradlex.javamodule.packaging.test.fixture;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class WritableFile {

    private final Path file;

    public WritableFile(Path file) {
        this.file = file;
    }

    public WritableFile(Directory parent, String filePath) {
        this.file = Io.unchecked(() -> Files.createDirectories(parent.getAsPath().resolve(filePath).getParent()))
                .resolve(Path.of(filePath).getFileName());
    }

    public WritableFile writeText(String text) {
        Io.unchecked(() -> Files.writeString(file, text, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
        return this;
    }

    public WritableFile appendText(String text) {
        Io.unchecked(() -> Files.writeString(file, text, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
        return this;
    }

    public void create() {
        Io.unchecked(() -> Files.createFile(file));
    }

    public WritableFile delete() {
        Io.unchecked(file, Files::delete);
        return this;
    }

    public boolean exists() {
        return Files.exists(file);
    }

    public Path getAsPath() {
        return file;
    }

    public String text() {
        return Io.unchecked(() -> Files.readString(file));
    }
}
