/*
  Copyright (c) 2018-present, SurfStudio LLC, Fedor Atyakshin.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package ru.surfstudio.android.analytics;

import ru.surfstudio.android.analytics.event.Event;
import ru.surfstudio.android.analytics.event.EventSender;

/**
 * Базовый сервис аналитики
 */
@Deprecated
public class BaseAnalyticsService {

    protected Analytics apiStore;

    public BaseAnalyticsService(Analytics apiStore) {
        this.apiStore = apiStore;
    }

    public void sendEvent(Event event) {
        create(event).send();
    }

    protected EventSender create(Event event) {
        return EventSender.create(apiStore, event);
    }
}
