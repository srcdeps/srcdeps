/**
 * Copyright 2015-2016 Maven Source Dependencies
 * Plugin contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.l2x6.srcdeps.localrepo;

import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.l2x6.srcdeps.core.BuildService;
import org.l2x6.srcdeps.core.fs.PathLocker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link LocalRepositoryManagerFactory} that produces {@link SrcdepsLocalRepositoryManager}. This is done through
 * looking up the {@link LocalRepositoryManagerFactory} implementations visible from the present Guice module, choosing
 * the one with the highest priority (ignoring {@link SrcdepsRepositoryManagerFactory}) and using that one as a
 * delegate.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@Named("srcdeps")
public class SrcdepsRepositoryManagerFactory implements LocalRepositoryManagerFactory {
    private static final Logger log = LoggerFactory.getLogger(SrcdepsRepositoryManagerFactory.class);
    public static final String SRCDEPS_REPOMANAGER_PRIORITY = "srcdeps.repomanager.priority";
    public static final float DEFAULT_SRCDEPS_REPOMANAGER_PRIORITY = 30;

    private final float priority;

    public SrcdepsRepositoryManagerFactory() {
        this.priority = Float.parseFloat(
                System.getProperty(SRCDEPS_REPOMANAGER_PRIORITY, String.valueOf(DEFAULT_SRCDEPS_REPOMANAGER_PRIORITY)));
    }

    /** See {@link #lookupDelegate()} */
    @Inject
    private Provider<Map<String, LocalRepositoryManagerFactory>> factories;

    /** Passed to {@link SrcdepsLocalRepositoryManager} */
    @Inject
    private BuildService buildService;

    /** Passed to {@link SrcdepsLocalRepositoryManager} */
    @Inject
    private PathLocker pathLocker;

    /** Passed to {@link SrcdepsLocalRepositoryManager} */
    @Inject
    private Provider<MavenSession> sessionProvider;

    /**
     * Looks up the delegate using {@link #lookupDelegate()}, calls
     * {@link SrcdepsRepositoryManagerFactory#newInstance(RepositorySystemSession, LocalRepository)} on the delegate
     * producing a delegate {@link LocalRepositoryManager} that is passed to
     * {@link SrcdepsLocalRepositoryManager#SrcdepsLocalRepositoryManager(LocalRepositoryManager, Provider, BuildService)}.
     * The new {@link SrcdepsLocalRepositoryManager} instance is then returned.
     *
     * @see org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory#newInstance(org.eclipse.aether.RepositorySystemSession,
     *      org.eclipse.aether.repository.LocalRepository)
     */
    @Override
    public LocalRepositoryManager newInstance(RepositorySystemSession session, LocalRepository repository)
            throws NoLocalRepositoryManagerException {
        LocalRepositoryManagerFactory delegate = lookupDelegate();

        log.debug("Creating a new SrcdepsLocalRepositoryManager");
        return new SrcdepsLocalRepositoryManager(delegate.newInstance(session, repository), sessionProvider,
                buildService, pathLocker);
    }

    /**
     * Returns the priority passed in {@value #SRCDEPS_REPOMANAGER_PRIORITY} system property or the default of
     * {@value #DEFAULT_SRCDEPS_REPOMANAGER_PRIORITY}.
     *
     * @see org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory#getPriority()
     */
    @Override
    public float getPriority() {
        return priority;
    }

    /**
     * Looks up the {@link LocalRepositoryManagerFactory} with highest priority (ignoring
     * {@link SrcdepsRepositoryManagerFactory}) using {@link #factories}. We need to do this lazily, because otherwise
     * there would be a circular dependency between {@link SrcdepsRepositoryManagerFactory} and the injected map of
     * {@link #factories}.
     *
     * @return the delegate factory
     */
    private LocalRepositoryManagerFactory lookupDelegate() {
        Map<String, LocalRepositoryManagerFactory> factoryImpls = factories.get();
        log.debug("SrcdepsRepositoryManagerFactory got {} LocalRepositoryManagerFactory instances",
                factoryImpls.size());

        LocalRepositoryManagerFactory winner = null;
        for (Entry<String, LocalRepositoryManagerFactory> en : factoryImpls.entrySet()) {
            LocalRepositoryManagerFactory factory = en.getValue();
            log.debug("SrcdepsRepositoryManagerFactory iterating over LocalRepositoryManagerFactory {}: {}",
                    en.getKey(), factory.getClass().getName());
            if (factory instanceof SrcdepsRepositoryManagerFactory) {
                /* ignore */
            } else if (winner == null || winner.getPriority() < factory.getPriority()) {
                winner = factory;
            }
        }
        log.info("SrcdepsLocalRepositoryManager will decorate {} with priority {}", winner.getClass().getName(), winner.getPriority());
        return winner;
    }
}
