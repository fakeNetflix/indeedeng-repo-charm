package com.indeed.charm.svn;

import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

import java.util.*;
import java.io.File;
import java.io.IOException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Maps;
import com.google.common.base.Splitter;
import com.indeed.charm.ReleaseEnvironment;
import com.indeed.charm.VCSClient;
import com.indeed.charm.VCSException;
import com.indeed.charm.model.LogEntry;
import com.indeed.charm.model.CommitInfo;
import com.indeed.charm.actions.LogEntryVisitor;
import com.indeed.charm.actions.DiffStatusVisitor;

/**
 */
public class SubversionClient implements VCSClient {
    private final ReleaseEnvironment env;
    private final String revisionUrlFormat;

    private final ISVNAuthenticationManager authManager;
    private final SVNClientManager clientManager;
    private final SVNRepository repo;

    static {
        // TODO: how to configure this in charm.properties
        DAVRepositoryFactory.setup();
    }

    public SubversionClient(ReleaseEnvironment env) throws VCSException {
        this(env, env.getUser(), env.getPassword());
    }

    public SubversionClient(ReleaseEnvironment env, String user, String password) throws VCSException {
        this.env = env;
        this.revisionUrlFormat = env.getRevisionUrlFormat();

        try {
            SVNURL url = SVNURL.parseURIDecoded(env.getRootUrl());
            repo = SVNRepositoryFactory.create(url, null);
            authManager = new BasicAuthenticationManager(user, password);
            repo.setAuthenticationManager(authManager);
            clientManager = SVNClientManager.newInstance(null, authManager);
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public void visitBranchDiffStatus(DiffStatusVisitor visitor, String project, String date1, String date2) throws VCSException {
        try {
            SVNDiffClient differ = new SVNDiffClient(authManager, null);
            differ.doDiffStatus(SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getBranchPath() + date1),
                    SVNRevision.HEAD,
                    SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getBranchPath() + date2),
                    SVNRevision.HEAD,
                    SVNDepth.INFINITY,
                    false,
                    new SVNDiffStatusVisitor(visitor));
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public void visitTagToTrunkDiffStatus(DiffStatusVisitor visitor, String project, String tag) throws VCSException {
        try {
            SVNDiffClient differ = new SVNDiffClient(authManager, null);
            differ.doDiffStatus(SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getTagPath() + tag),
                    SVNRevision.HEAD,
                    SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getTrunkPath()),
                    SVNRevision.HEAD,
                    SVNDepth.INFINITY,
                    false,
                    new SVNDiffStatusVisitor(visitor));
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public void visitBranchToTrunkDiffStatus(DiffStatusVisitor visitor, String project, String branchDate) throws VCSException {
        try {
            SVNDiffClient differ = new SVNDiffClient(authManager, null);
            differ.doDiffStatus(SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getBranchPath() + branchDate),
                    SVNRevision.HEAD,
                    SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getTrunkPath()),
                    SVNRevision.HEAD,
                    SVNDepth.INFINITY,
                    false,
                    new SVNDiffStatusVisitor(visitor));
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public void visitTagDiffStatus(DiffStatusVisitor visitor, String project, String version1, String version2) throws VCSException {
        try {
            SVNDiffClient differ = new SVNDiffClient(authManager, null);
            differ.doDiffStatus(SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getTagPath() + version1),
                    SVNRevision.HEAD,
                    SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getTagPath() + version2),
                    SVNRevision.HEAD,
                    SVNDepth.INFINITY,
                    false,
                    new SVNDiffStatusVisitor(visitor));
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public void visitTrunkChangeLog(LogEntryVisitor visitor, String project, int limit, String... paths) throws VCSException {
        try {
            final SVNURL trunkUrl = SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getTrunkPath());
            final SVNLogClient logClient = new SVNLogClient(authManager, null);
            final SVNRevision revStart = SVNRevision.create(0);
            logClient.doLog(trunkUrl, paths, revStart, revStart, SVNRevision.HEAD, true, false, limit, new SVNLogEntryVisitor(visitor, revisionUrlFormat));
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public void visitBranchChangeLog(LogEntryVisitor visitor, String project, String branchDate, int limit, String... paths) throws VCSException {
        try {
            final SVNURL branchUrl = SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getBranchPath() + branchDate);
            final SVNLogClient logClient = new SVNLogClient(authManager, null);
            final SVNRevision revStart = SVNRevision.create(0);
            logClient.doLog(branchUrl, paths, revStart, revStart, SVNRevision.HEAD, true, false, limit, new SVNLogEntryVisitor(visitor, revisionUrlFormat));
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public void visitTagChangeLog(LogEntryVisitor visitor, String project, String tag, int limit, String... paths) throws VCSException {
        try {
            final SVNURL tagUrl = SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getTagPath() + tag);
            final SVNLogClient logClient = new SVNLogClient(authManager, null);
            final SVNRevision revStart = SVNRevision.create(0);
            logClient.doLog(tagUrl, paths, revStart, revStart, SVNRevision.HEAD, true, false, limit, new SVNLogEntryVisitor(visitor, revisionUrlFormat));
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public long getBranchStartRevision(String project, String branchDate) throws VCSException {
        final long[] rev = new long[1];
        visitBranchChangeLog(new LogEntryVisitor() {
            public void visit(LogEntry entry) {
                rev[0] = entry.getRevision();
            }
        }, project, branchDate, 1, ".");
        return rev[0];
    }

    public void visitTrunkChangeLogSinceBranch(LogEntryVisitor visitor, String project, String branchDate, int limit, String... paths) throws VCSException {
        try {
            final SVNURL trunkUrl = SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getTrunkPath());
            final SVNLogClient logClient = new SVNLogClient(authManager, null);
            final SVNRevision revStart = SVNRevision.create(getBranchStartRevision(project, branchDate));
            logClient.doLog(trunkUrl, paths, revStart, revStart, SVNRevision.HEAD, true, false, limit, new SVNLogEntryVisitor(visitor, revisionUrlFormat));
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public long getTagFirstRevision(String project, String tag) throws VCSException {
        final long[] rev = new long[1];
        visitTagChangeLog(new LogEntryVisitor() {
            public void visit(LogEntry entry) {
                rev[0] = entry.getRevision();
            }
        }, project, tag, 1, ".");
        return rev[0];
    }

    public void visitTrunkChangeLogSinceTag(LogEntryVisitor visitor, String project, String tag, int limit, String... paths) throws VCSException {
        try {
            final SVNURL trunkUrl = SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getTrunkPath());
            final SVNLogClient logClient = new SVNLogClient(authManager, null);
            final SVNRevision revStart = SVNRevision.create(getTagFirstRevision(project, tag));
            logClient.doLog(trunkUrl, paths, revStart, revStart, SVNRevision.HEAD, true, false, limit, new SVNLogEntryVisitor(visitor, revisionUrlFormat));
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    private static Splitter versionSplitter = Splitter.on(".");

    private static String normalizeVersion(String in, int width) {
        final String format = "%" + width + "s";
        StringBuilder builder = new StringBuilder();
        for (String v : versionSplitter.split(in)) {
            builder.append(String.format(format, v)).append(".");
        }
        return builder.toString();
    }

    private static Comparator<? super SVNDirEntry> REVERSE_VERSION_COMPARATOR =
            new Comparator<SVNDirEntry>() {
                public int compare(SVNDirEntry v1, SVNDirEntry v2) {
                    String s1 = normalizeVersion(v1.getName(), 4);
                    String s2 = normalizeVersion(v2.getName(), 4);
                    return s2.compareTo(s1);
                }
            };

    private static Comparator<? super SVNDirEntry> REVERSE_BRANCH_COMPARATOR =
            new Comparator<SVNDirEntry>() {
                public int compare(SVNDirEntry b1, SVNDirEntry b2) {
                    String s1 = b1.getName();
                    String s2 = b2.getName();
                    return s2.compareTo(s1);
                }
            };

    private static EnumMap<Ordering, Comparator<? super SVNDirEntry>> orderings = Maps.newEnumMap(Ordering.class);

    static {
        orderings.put(Ordering.NORMAL, com.google.common.collect.Ordering.natural());
        orderings.put(Ordering.REVERSE_BRANCH, REVERSE_BRANCH_COMPARATOR);
        orderings.put(Ordering.REVERSE_VERSION, REVERSE_VERSION_COMPARATOR);
    }

    public List<String> listBranches(String project, int limit, Ordering ordering) throws VCSException {
        try {
            final TreeSet<SVNDirEntry> branches = Sets.newTreeSet(orderings.get(ordering));
            repo.getDir(project + env.getBranchPath(), -1, null, branches);
            Iterator<SVNDirEntry> branchesIter = branches.iterator();
            List<String> ret = Lists.newArrayListWithExpectedSize(limit);
            for (int i = 0; i < limit && branchesIter.hasNext(); i++) {
                ret.add(branchesIter.next().getName());
            }
            return ret;
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public List<String> listTags(String project, int limit, Ordering ordering) throws VCSException {
        try {
            final TreeSet<SVNDirEntry> tags = Sets.newTreeSet(orderings.get(ordering));
            repo.getDir(project + env.getTagPath(), -1, null, tags);
            Iterator<SVNDirEntry> tagsIter = tags.iterator();
            List<String> ret = Lists.newArrayListWithExpectedSize(limit);
            for (int i = 0; i < limit && tagsIter.hasNext(); i++) {
                ret.add(tagsIter.next().getName());
            }
            return ret;
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public List<String> listDir(String dir, Ordering ordering) throws VCSException {
        try {
            final TreeSet<SVNDirEntry> entries = Sets.newTreeSet(orderings.get(ordering));
            repo.getDir(dir, -1, null, entries);
            List<String> ret = Lists.newArrayListWithExpectedSize(entries.size());
            for (SVNDirEntry entry : entries) {
                ret.add(entry.getName());
            }
            return ret;
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public boolean hasFilesSince(String path, final Date earliest) {
        final boolean[] result = new boolean[]{false};
        try {
            repo.getDir(path, -1, null, new ISVNDirEntryHandler() {
                public void handleDirEntry(SVNDirEntry svnDirEntry) throws SVNException {
                    if (svnDirEntry.getDate().after(earliest)) {
                        result[0] = true;
                        // stop iteration
                        throw new SVNException(SVNErrorMessage.UNKNOWN_ERROR_MESSAGE);
                    }
                }
            });
        } catch (SVNException e) {
            // ignore
        }
        return result[0];
    }

    public boolean checkExistsInHead(String path) throws VCSException {
        try {
            SVNNodeKind kind = repo.checkPath(path, repo.getLatestRevision());
            return kind != SVNNodeKind.NONE;
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public List<String> getTags(String project, int limit, Ordering ordering) throws VCSException {
        try {
            final TreeSet<SVNDirEntry> tags = Sets.newTreeSet(orderings.get(ordering));
            repo.getDir(project + env.getTagPath(), -1, null, tags);
            Iterator<SVNDirEntry> tagsIter = tags.iterator();
            List<String> ret = Lists.newArrayListWithExpectedSize(limit);
            for (int i = 0; i < limit && tagsIter.hasNext(); i++) {
                ret.add(tagsIter.next().getName());
            }
            return ret;
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public long checkoutBranch(String project, String branchDate, File workingDir) throws VCSException, IOException {
        try {
            final SVNUpdateClient updateClient = clientManager.getUpdateClient();
            final SVNURL branchUrl = SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getBranchPath() + branchDate);
            final SVNRevision revStart = SVNRevision.create(getBranchStartRevision(project, branchDate));
            return updateClient.doCheckout(branchUrl, workingDir, revStart, SVNRevision.HEAD, SVNDepth.INFINITY, false);
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public CommitInfo mergeToBranch(String project, long revision, String branchDate, String messagePrefix, File workingDir) throws VCSException, IOException {
        try {
            final long branchRev = checkoutBranch(project, branchDate, workingDir);
            if (branchRev > 0) {
                final SVNDiffClient diffClient = clientManager.getDiffClient();
                final SVNURL url = SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getTrunkPath());
                final SVNRevision pegRev = SVNRevision.create(branchRev);
                final SVNRevision prevRev = SVNRevision.create(revision - 1);
                final SVNRevision rev = SVNRevision.create(revision);
                diffClient.doMerge(url, pegRev, prevRev, rev, workingDir, true, true, false, false);

                final String rlogMessage = getTrunkLogEntry(project, revision).getLogMessage();
                final String commitMessage = messagePrefix + " merged r" + revision + ": " + rlogMessage;
                final SVNCommitClient commitClient = clientManager.getCommitClient();
                return new SVNCommitInfoWrapper(commitClient.doCommit(new File[]{workingDir}, false, commitMessage, null, null, false, false, SVNDepth.INFINITY));
            }
            return null;
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public LogEntry getTrunkLogEntry(String project, long revision) throws VCSException {
        try {
            final SVNLogClient logClient = clientManager.getLogClient();
            final SVNURL url = SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getTrunkPath());
            final SVNRevision rev = SVNRevision.create(revision);
            final LogEntry[] entry = new SVNLogEntryWrapper[1];
            logClient.doLog(url, new String[]{"."}, rev, rev, rev, true, true, false, 1, null,
                    new ISVNLogEntryHandler() {
                        public void handleLogEntry(SVNLogEntry svnLogEntry) throws SVNException {
                            entry[0] = new SVNLogEntryWrapper(svnLogEntry, env.getRevisionUrlFormat());
                        }
                    });
            return entry[0];
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public LogEntry getBranchLogEntry(String project, String branchDate, long revision) throws VCSException {
        try {
            final SVNLogClient logClient = clientManager.getLogClient();
            final SVNURL url = SVNURL.parseURIDecoded(env.getRootUrl() + project + env.getBranchPath() + branchDate);
            final SVNRevision rev = SVNRevision.create(revision);
            final LogEntry[] entry = new SVNLogEntryWrapper[1];
            logClient.doLog(url, new String[]{"."}, rev, rev, rev, true, true, false, 1, null,
                    new ISVNLogEntryHandler() {
                        public void handleLogEntry(SVNLogEntry svnLogEntry) throws SVNException {
                            entry[0] = new SVNLogEntryWrapper(svnLogEntry, env.getRevisionUrlFormat());
                        }
                    });
            return entry[0];
        } catch (SVNException e) {
            throw new VCSException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        VCSClient sc = new SubversionClient(new ReleaseEnvironment());

//        final ISVNDiffStatusHandler dsHandler = new ISVNDiffStatusHandler() {
//            public void handleDiffStatus(SVNDiffStatus svnDiffStatus) throws VCSException {
//                System.out.println(svnDiffStatus.getModificationType() + " " + svnDiffStatus.getPath());
//            }
//        };
//
//        List<String> lastTwoBranches = sc.listBranches(args[0], 2, REVERSE_BRANCH_COMPARATOR);
//        System.out.println(lastTwoBranches);
//        sc.visitBranchDiffStatus("jasx", lastTwoBranches.get(1), lastTwoBranches.get(0), dsHandler);
//
//        System.out.println();
//        List<String> lastTwoTags = sc.getTags(args[0], 2, REVERSE_VERSION_COMPARATOR);
//        System.out.println(lastTwoTags);
//        sc.visitTagDiffStatus("jasx", lastTwoTags.get(1), lastTwoTags.get(0), dsHandler);

//        sc.visitTrunkChangeLogSinceBranch(new ISVNLogEntryHandler() {
//            public void handleLogEntry(SVNLogEntryWrapper svnLogEntry) throws VCSException {
//                System.out.println(svnLogEntry.toString());
//            }
//        }, args[0], args[1], Integer.parseInt(args[2]), args[3]);

//        System.out.println(sc.checkoutBranch(args[0], args[1], new File(args[2])));

//        System.out.println(sc.getTrunkLogEntry(args[0], Long.parseLong(args[1])).getChangedPaths().values());

//        File tmpDir = Files.createTempDir();
//        System.out.println(tmpDir);
//        System.out.println(sc.mergeToBranch(args[0], Long.parseLong(args[1]), args[2], "NOBUG ", tmpDir));
//        Files.deleteRecursively(tmpDir);
    }

}