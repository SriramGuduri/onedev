package com.pmease.gitplex.core.manager.impl;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.criterion.Restrictions;

import com.pmease.commons.hibernate.Sessional;
import com.pmease.commons.hibernate.Transactional;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.hibernate.dao.EntityCriteria;
import com.pmease.gitplex.core.manager.PullRequestManager;
import com.pmease.gitplex.core.manager.VerificationManager;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.Verification;
import com.pmease.gitplex.core.model.Verification.Status;

@Singleton
public class DefaultVerificationManager implements VerificationManager {

	private final Dao dao;
	
	private final PullRequestManager pullRequestManager;

	@Inject
	public DefaultVerificationManager(Dao dao, PullRequestManager pullRequestManager) {
		this.dao = dao;
		this.pullRequestManager = pullRequestManager;
	}

	@Sessional
	@Override
	public Collection<Verification> findBy(PullRequest request, String commit) {
		return dao.query(EntityCriteria.of(Verification.class)
				.add(Restrictions.eq("request", request))
				.add(Restrictions.eq("commit", commit)), 0, 0);
	}

	@Sessional
	@Override
	public Verification findBy(PullRequest request, String commit, String configuration) {
		return dao.find(EntityCriteria.of(Verification.class)
				.add(Restrictions.eq("request", request))
				.add(Restrictions.eq("commit", commit))
				.add(Restrictions.eq("configuration", configuration)));
	}

	@Transactional
	@Override
	public void save(Verification verification) {
		dao.persist(verification);

		onVerificationChange(verification.getCommit());
	}

	@Transactional
	@Override
	public void delete(Verification verification) {
		dao.remove(verification);
		
		onVerificationChange(verification.getCommit());
	}
	
	private void onVerificationChange(String commit) {
		for (PullRequest request : pullRequestManager.findByCommit(commit))
			pullRequestManager.onGateKeeperUpdate(request);
	}

	@Override
	public Status getOverallStatus(Collection<Verification> verifications) {
		Verification.Status overallStatus = null;
		for (Verification verification: verifications) {
			if (verification.getStatus() == Verification.Status.NOT_PASSED) {
				overallStatus = Verification.Status.NOT_PASSED;
				break;
			} else if (verification.getStatus() == Verification.Status.ONGOING) {
				overallStatus = Verification.Status.ONGOING;
			} else if (overallStatus == null) {
				overallStatus = Verification.Status.PASSED;
			}
		}
		return overallStatus;
	}

}
