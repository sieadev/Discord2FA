package dev.siea.discord2fa.common.versioning;

/**
 * Result of checking for updates. Used by the server to decide what to log.
 */
public final class UpdateCheckResult {

    private final int versionsBehind;
    private final String downloadUrl;

    public UpdateCheckResult(int versionsBehind, String downloadUrl) {
        this.versionsBehind = Math.max(0, versionsBehind);
        this.downloadUrl = downloadUrl != null ? downloadUrl : "";
    }

    /** Number of releases behind the latest (0 = up to date). */
    public int getVersionsBehind() {
        return versionsBehind;
    }

    public boolean isUpToDate() {
        return versionsBehind == 0;
    }

    /** URL where users can download the latest version. */
    public String getDownloadUrl() {
        return downloadUrl;
    }
}
