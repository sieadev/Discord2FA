package dev.siea.discord2fa.common.versioning;

/**
 * Result of checking for updates. Used by the server to decide what to log.
 */
public final class UpdateCheckResult {

    private final int versionsBehind;
    private final String downloadUrl;
    private final boolean newerThanLatest;
    private final String latestReleaseVersion;

    public UpdateCheckResult(int versionsBehind, String downloadUrl) {
        this(versionsBehind, downloadUrl, false, null);
    }

    public UpdateCheckResult(int versionsBehind, String downloadUrl, boolean newerThanLatest, String latestReleaseVersion) {
        this.versionsBehind = Math.max(0, versionsBehind);
        this.downloadUrl = downloadUrl != null ? downloadUrl : "";
        this.newerThanLatest = newerThanLatest;
        this.latestReleaseVersion = latestReleaseVersion != null ? latestReleaseVersion : "";
    }

    /** Number of releases behind the latest (0 = up to date or experimental). */
    public int getVersionsBehind() {
        return versionsBehind;
    }

    public boolean isUpToDate() {
        return versionsBehind == 0 && !newerThanLatest;
    }

    /** True if current version is newer than the latest release (e.g. dev/experimental build). */
    public boolean isNewerThanLatest() {
        return newerThanLatest;
    }

    /** Latest release version from the update source (e.g. "2.0.0"); empty if unknown. */
    public String getLatestReleaseVersion() {
        return latestReleaseVersion;
    }

    /** URL where users can download the latest version. */
    public String getDownloadUrl() {
        return downloadUrl;
    }
}
