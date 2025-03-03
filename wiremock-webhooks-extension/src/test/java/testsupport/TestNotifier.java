/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package testsupport;

import com.github.tomakehurst.wiremock.common.LocalNotifier;
import com.github.tomakehurst.wiremock.common.Notifier;

import java.util.ArrayList;
import java.util.List;

public class TestNotifier implements Notifier {

    private List<String> info;
    private List<String> error;

    private Notifier previousNotifier;

    public TestNotifier() {
        this.info = new ArrayList<>();
        this.error = new ArrayList<>();
    }

    public static TestNotifier createAndSet() {
        TestNotifier testNotifier = new TestNotifier();
        testNotifier.previousNotifier = LocalNotifier.notifier();
        LocalNotifier.set(testNotifier);
        return testNotifier;
    }

    public void revert() {
        LocalNotifier.set(previousNotifier);
    }

    @Override
    public void info(String message) {
        this.info.add(message);
    }

    @Override
    public void error(String message) {
        this.error.add(message);
    }

    @Override
    public void error(String message, Throwable t) {
        this.error.add(message);
    }

    public List<String> getInfoMessages() { return info; }

    public List<String> getErrorMessages() { return error; }

    public void reset() {
        info.clear();
        error.clear();
    }
}
