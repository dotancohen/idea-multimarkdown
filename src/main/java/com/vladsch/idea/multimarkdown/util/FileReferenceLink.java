/*
 * Copyright (c) 2015-2015 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.vladsch.idea.multimarkdown.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FileReferenceLink extends FileReference {

    public static final int REASON_TARGET_HAS_SPACES = 1;
    public static final int REASON_CASE_MISMATCH = 2;
    public static final int REASON_WIKI_PAGEREF_HAS_DASHES = 4;
    public static final int REASON_NOT_UNDER_WIKI_HOME = 8;
    public static final int REASON_TARGET_NOT_WIKI_PAGE_EXT = 16;
    public static final int REASON_NOT_UNDER_SOURCE_WIKI_HOME = 32;
    public static final int REASON_TARGET_NAME_HAS_ANCHOR = 64;
    public static final int REASON_TARGET_PATH_HAS_ANCHOR = 128;

    protected final @NotNull FileReference sourceReference;
    protected String pathPrefix;
    protected boolean wikiAccessible;
    protected int upDirectories;
    protected int downDirectories;

    public FileReferenceLink(@NotNull String sourcePath, @NotNull String targetPath, Project project) {
        super(targetPath, project);
        this.sourceReference = new FileReference(sourcePath, project);

        computeLinkRefInfo();
    }

    public FileReferenceLink(@NotNull FileReference sourceReference, @NotNull FileReference targetReference) {
        super(targetReference);

        assert sourceReference.getProject() == targetReference.getProject();
        this.sourceReference = sourceReference;

        computeLinkRefInfo();
    }

    public FileReferenceLink(@NotNull VirtualFile sourceFile, @NotNull FileReference targetReference) {
        super(targetReference);

        this.sourceReference = new FileReference(sourceFile, project);

        computeLinkRefInfo();
    }

    public FileReferenceLink(@NotNull FileReference sourceReference, @NotNull VirtualFile targetFile) {
        super(new FileReference(targetFile, sourceReference.project));

        this.sourceReference = sourceReference;

        computeLinkRefInfo();
    }

    public FileReferenceLink(@NotNull VirtualFile sourceFile, @NotNull VirtualFile targetFile, Project project) {
        super(new FileReference(targetFile, project));

        this.sourceReference = new FileReference(sourceFile, project);

        computeLinkRefInfo();
    }

    public FileReferenceLink(@NotNull PsiFile sourceFile, @NotNull PsiFile targetFile) {
        super(new FileReference(targetFile));

        assert sourceFile.getProject() == targetFile.getProject();
        this.sourceReference = new FileReference(sourceFile);

        computeLinkRefInfo();
    }

    public FileReferenceLink(@NotNull FileReference sourceReference, @NotNull PsiFile targetFile) {
        super(new FileReference(targetFile));

        assert sourceReference.getProject() == targetFile.getProject();
        this.sourceReference = sourceReference;

        computeLinkRefInfo();
    }

    public FileReferenceLink(@NotNull PsiFile sourceFile, @NotNull FileReference targetReference) {
        super(targetReference);

        assert sourceFile.getProject() == targetReference.getProject();
        this.sourceReference = new FileReference(sourceFile);

        computeLinkRefInfo();
    }

    @NotNull
    public FileReference getSourceReference() {
        return sourceReference;
    }

    @NotNull
    public String getLinkRef() {
        return pathPrefix + getFileName();
    }

    @NotNull
    public String getLinkRefNoExt() {
        return pathPrefix + getFileNameNoExt();
    }

    @NotNull
    public String getWikiPageRef() {
        return FilePathInfo.asWikiRef(pathPrefix) + getFileNameNoExtAsWikiRef();
    }

    @NotNull
    public String getWikiPageRefWithAnchor() {
        return FilePathInfo.asWikiRef(pathPrefix) + getFileNameWithAnchorNoExtAsWikiRef();
    }

    @NotNull
    public String getLinkRefWithAnchor() {
        return pathPrefix + getFileNameWithAnchor();
    }

    @NotNull
    public String getLinkRefWithAnchorNoExt() {
        return pathPrefix + getFileNameWithAnchorNoExt();
    }

    @NotNull
    public String getPathPrefix() {
        return pathPrefix;
    }

    public boolean isWikiAccessible() {
        return wikiAccessible;
    }

    public boolean linkRefHasSpaces() {
        return getLinkRef().indexOf(' ') >= 0;
    }

    public boolean linkRefWithoutExtHasSpaces() {
        return getLinkRefNoExt().indexOf(' ') >= 0;
    }

    public int getUpDirectories() {
        return upDirectories;
    }

    public int getDownDirectories() {
        return downDirectories;
    }

    public static class InaccessibleWikiPageReasons {
        final int reasons;
        final String wikiRef;
        final FileReferenceLink referenceLink;

        InaccessibleWikiPageReasons(int reasons, String wikiRef, FileReferenceLink referenceLink) {
            this.reasons = reasons;
            this.wikiRef = wikiRef;
            this.referenceLink = referenceLink;
        }

        public boolean targetNameHasSpaces() { return (reasons & REASON_TARGET_HAS_SPACES) != 0; }
        public String targetNameHasSpacedFixed() { return referenceLink.getFileName().replace(' ', '-'); }

        public boolean caseMismatchOnly() { return (reasons & REASON_CASE_MISMATCH) != 0 && reasons == REASON_CASE_MISMATCH; }
        public boolean caseMismatch() { return (reasons & REASON_CASE_MISMATCH) != 0; }
        public String caseMismatchWikiRefFixed() { return referenceLink.getLinkRefNoExt().replace('-', ' '); }
        public String caseMismatchFileNameFixed() { return FilePathInfo.wikiRefAsFileNameWithExt(new FilePathInfo(wikiRefHasDashesFixed()).getFileNameNoExt()); }

        public boolean wikiRefHasDashes() { return (reasons & REASON_WIKI_PAGEREF_HAS_DASHES) != 0; }
        public String wikiRefHasDashesFixed() { return wikiRef.replace('-', ' '); }

        public boolean targetNotWikiPageExt() { return (reasons & REASON_TARGET_NOT_WIKI_PAGE_EXT) != 0; }
        public String targetNotWikiPageExtFixed() { return referenceLink.getFileNameNoExt() + WIKI_PAGE_EXTENSION; }

        public boolean targetNotInWikiHome() { return (reasons & REASON_NOT_UNDER_WIKI_HOME) != 0; }
        public String targetNotInWikiHomeFixed() { return referenceLink.getSourceReference().getPath() + referenceLink.getFileName(); }

        public boolean targetNotInSameWikiHome() { return (reasons & REASON_NOT_UNDER_SOURCE_WIKI_HOME) != 0; }
        public String targetNotInSameWikiHomeFixed() { return referenceLink.getSourceReference().getPath() + referenceLink.getFileName(); }

        public boolean targetNameHasAnchor() { return (reasons & REASON_TARGET_NAME_HAS_ANCHOR) != 0; }
        public String targetNameHasAnchorFixed() { return referenceLink.getFileNameWithAnchor().replace("#", ""); }

        public boolean targetPathHasAnchor() { return (reasons & REASON_TARGET_PATH_HAS_ANCHOR) != 0; }
    }

    @NotNull
    public InaccessibleWikiPageReasons inaccessibleWikiPageRefReasons(@Nullable String wikiPageRef) {
        int reasons = 0;

        if (linkRefHasSpaces()) reasons |= REASON_TARGET_HAS_SPACES;
        if (wikiPageRef != null && wikiPageRef.replace('-', ' ').equalsIgnoreCase(this.getWikiPageRef().replace('-', ' ')) && !wikiPageRef.replace('-', ' ').equals(this.getWikiPageRef().replace('-', ' ')))
            reasons |= REASON_CASE_MISMATCH;
        if (wikiPageRef != null && wikiPageRef.indexOf('-') >= 0) reasons |= REASON_WIKI_PAGEREF_HAS_DASHES;

        if (sourceReference.isWikiPage()) {
            if (!isUnderWikiHome()) reasons |= REASON_NOT_UNDER_WIKI_HOME;
            else if (!getWikiHome().startsWith(sourceReference.getWikiHome())) reasons |= REASON_NOT_UNDER_SOURCE_WIKI_HOME;

            if (!hasWikiPageExt()) reasons |= REASON_TARGET_NOT_WIKI_PAGE_EXT;
        }

        if (pathContainsAnchor()) reasons |= REASON_TARGET_PATH_HAS_ANCHOR;
        if (fileNameContainsAnchor()) reasons |= REASON_TARGET_NAME_HAS_ANCHOR;

        return new InaccessibleWikiPageReasons(reasons, wikiPageRef, this);
    }

    protected void computeLinkRefInfo() {
        pathPrefix = "";
        String[] targetParts = getFilePath().split("/");
        String[] sourceParts = sourceReference.getFilePath().split("/");
        downDirectories = 0;
        upDirectories = 0;

        int iMax = Math.min(targetParts.length - 1, sourceParts.length - 1);
        int i;

        for (i = 1; i < iMax; i++) {
            if (!targetParts[i].equals(sourceParts[i])) break;
        }

        // used up the common prefix, now for every source we need to add ../
        iMax = sourceParts.length - 1;
        for (int j = i; j < iMax; j++) {
            pathPrefix += "../";
            upDirectories++;
        }

        // used up the common prefix, now for every target we need to add the part/
        iMax = targetParts.length - 1;
        for (; i < iMax; i++) {
            pathPrefix += targetParts[i] + "/";
            downDirectories++;
        }

        wikiAccessible = getLinkRef().indexOf(' ') < 0 && hasWikiPageExt() && getWikiHome().startsWith(sourceReference.getWikiHome()) && !containsAnchor();
    }

    public int reflinkCompareTo(@NotNull FileReferenceLink o) {
        if (upDirectories != o.upDirectories) return upDirectories - o.upDirectories;
        if (downDirectories != o.downDirectories) return downDirectories - o.downDirectories;
        return getLinkRef().compareTo(o.getLinkRef());
    }

    @Override
    public int compareTo(FilePathInfo o) {
        return !(o instanceof FileReferenceLink) ||
                (
                        sourceReference == ((FileReferenceLink) o).sourceReference &&
                                getLinkRef().equals(((FileReferenceLink) o).getLinkRef()) &&
                                getLinkRefWithAnchor().equals(((FileReferenceLink) o).getLinkRefWithAnchor()) &&
                                getLinkRefNoExt().equals(((FileReferenceLink) o).getLinkRefNoExt()) &&
                                getWikiPageRef().equals(((FileReferenceLink) o).getWikiPageRef()) &&
                                getWikiPageRefWithAnchor().equals(((FileReferenceLink) o).getWikiPageRefWithAnchor()) &&
                                wikiAccessible == ((FileReferenceLink) o).wikiAccessible &&
                                upDirectories == ((FileReferenceLink) o).upDirectories &&
                                downDirectories == ((FileReferenceLink) o).downDirectories
                )
                ? super.compareTo(o) : reflinkCompareTo((FileReferenceLink) o);
    }

    @Override
    public String toString() {
        return "FileReferenceLink(" +
                innerString() +
                ")";
    }

    @Override
    public String innerString() {
        return super.innerString() +
                "sourceReference  = " + sourceReference + ", " +
                "linkRef = " + "'" + getLinkRef() + "', " +
                "linkRefNoExt = " + "'" + getLinkRefNoExt() + "', " +
                "linkRefWithAnchor = " + "'" + getLinkRefWithAnchor() + "', " +
                "linkRefWithAnchorNoExt = " + "'" + getLinkRefWithAnchorNoExt() + "', " +
                "wikiPageRef = " + "'" + getWikiPageRef() + "', " +
                "wikiPageRefWithAnchor = " + "'" + getWikiPageRefWithAnchor() + "', " +
                "wikiAccessible  = " + wikiAccessible + ", " +
                "upDirectories  = " + upDirectories + ", " +
                "downDirectories  = " + downDirectories + ", " +
                "";
    }
}
