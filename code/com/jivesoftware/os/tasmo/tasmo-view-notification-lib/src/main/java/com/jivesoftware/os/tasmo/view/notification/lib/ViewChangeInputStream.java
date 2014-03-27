/*
 * Copyright 2014 pete.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jivesoftware.os.tasmo.view.notification.lib;

import com.jivesoftware.os.jive.utils.base.interfaces.CallbackStream;
import com.jivesoftware.os.tasmo.lib.EventWrite;
import java.util.List;

/**
 *
 * @author pete
 */
public class ViewChangeInputStream implements CallbackStream<List<EventWrite>> {

    private final ViewChangeNotifier viewChangeNotifier;
    private final ViewChangeNotificationProcessor viewChangeNotificationProcessor;

    public ViewChangeInputStream(ViewChangeNotifier viewChangeNotifier, ViewChangeNotificationProcessor viewChangeNotificationProcessor) {
        this.viewChangeNotifier = viewChangeNotifier;
        this.viewChangeNotificationProcessor = viewChangeNotificationProcessor;
    }

    @Override
    public List<EventWrite> callback(List<EventWrite> value) throws Exception {
        if (value != null) {
            viewChangeNotifier.notifyChangedViews(value, viewChangeNotificationProcessor);
        }
        return value;
    }
}
