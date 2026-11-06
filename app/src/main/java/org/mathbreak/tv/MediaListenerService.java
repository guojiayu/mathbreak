package org.mathbreak.tv;

import android.service.notification.NotificationListenerService;

/** Permission gate for MediaSessionManager.getActiveSessions; no notification scraping. */
public final class MediaListenerService extends NotificationListenerService {}
