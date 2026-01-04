import React, { useEffect, useState } from 'react';
import { 
  RepositoriesTabHeading,
  RepositoriesTabTitle,
  RepositoriesTabWrapper,
  AddRepoButton,
  RepositoriesDivider,
  RepositoriesTable,
  RepositoryRow,
} from './styles/DashboardBody.styles';
import { useAuth } from '../hooks/AuthContext';
import api from '../api/api';
import RepoListItem from './RepoListItem';

function Repositories() {
  const { user } = useAuth();
  const [repositories, setRepositories] = useState([]);
  const handleAddRepository = () => {
    const url = `https://github.com/apps/clean-pr/installations/new?target_id=${user.userId}`;
    // window.location.href = url;
    window.open(url, "_blank")
  };

  // Remove repository handler
  const handleRemoveRepository = async (repoId, installationId) => {
    try {
      await api.delete(`/repository/${repoId}/installation/${installationId}`);
      setRepositories(repositories.filter(r => r.id !== repoId));
    } catch (err) {
      console.log(err);
    }
  };

  // fetch all the repo with user id
  useEffect(() => {
    api.get(`/repository/user/${user.userId}`)
      .then(res => {
        setRepositories(res.data);
      })
      .catch(err => {
        console.log(err);
      });
  }, [user.userId]);

  // Button label logic
  const addRepoLabel = repositories.length === 0
    ? "Add Repo"
    : (
      <>
        Add <span style={{ color: "#bbaaff", fontWeight: 600 }}>/</span> Remove Repos
      </>
    );

  return (
    <RepositoriesTabWrapper>
      <RepositoriesTabHeading>
        <RepositoriesTabTitle>Repositories</RepositoriesTabTitle>
        <AddRepoButton onClick={handleAddRepository}>
          <svg width="18" height="18" fill="none" viewBox="0 0 24 24">
            <path d="M12 5v14m7-7H5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          {addRepoLabel}
        </AddRepoButton>
      </RepositoriesTabHeading>

      <RepositoriesDivider>
        <span>Your Repositories</span>
      </RepositoriesDivider>

      <RepositoriesTable>
        {repositories.length === 0 && (
          <RepositoryRow style={{ justifyContent: 'center', color: '#aaa', fontStyle: 'italic' }}>
            No repositories found.
          </RepositoryRow>
        )}
        {repositories.map(repo => (
          <RepoListItem key={repo.id} repo={repo} onRemove={handleRemoveRepository} />
        ))}
      </RepositoriesTable>
    </RepositoriesTabWrapper>
  );
}

export default Repositories;
