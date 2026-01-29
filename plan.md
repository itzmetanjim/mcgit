Features to add:
> @: Requires world change command
> #: Requires other commands as well
- DONE:  `/git init <name>` Initialize a new MCGit repository.
- DONE:  `/git activate <name>` Switch to the specified repository.
- DONE:  `/git add <coords>` Add one block at the specified coordinates to the staging area.
- DONE:  `/git add <coords> <coords> [|hollow|outline]` Add a cuboid of blocks to the staging area
- DONE: @`/git rm ...` Remove blocks/entities from the staging area and revert them. NOTE: Changing blocks/entities in the world needs to be done by issuing commands, as this is a clientside mod.
- DONE:  `/git unstage ...`  does not revert , only removes from staging area.
- DONE:  `/git commit [-m] "message"` Commit the staged changes with a message. The `-m` flag does nothing.
- DONE: @`/git revert ...` `/git reset` but `--hard`, `...` is a commit hash. This does change the world.
- DONE:  `/git reset [commit-hash]` Reset to a specific commit hash or the latest commit. This does not revert the actual world.
- DONE:  `/git status` Show the current status of the repository, including staged changes, unstaged changes, but not untracked blocks/entities.
- DONE:  `/git listCommits` Lists all commits in the current branch.
- DONE:  `/git listRepos` Lists all available repositories.
- DONE:  `/git autoadd [toggle|on|off]` Enable or disable automatic addition of changes to the staging area. To quickly switch between, use an enchanted red wool in offhand to make this act toggled (so if you have it in offhand, auto add is off, otherwise on).
- DONE:  `/git autorm [toggle|on|off]` Enable or disable automatic removal of deleted blocks/entities from the staging area. To quickly switch between, use an enchanted red wool in offhand to make this act toggled (so if you have it in offhand, autorm is off, otherwise on).
- DONE: @`/git clone <url/local> <name>` Clone a remote repository from the specified URL into a new local repository with the given name.
          URL can be a local repository name (like `myrepo`), or a GitHub `author/repo` (like `octocat/Hello-World`) or a full URL to a git repository (like `https://github.com/octocat/Hello-World.git`).
- DONE:  `/git clonesoft <url/local> <name>` `/git clone` but do not put it in the world
- DONE: @`/git put <url/local>` `/git clone` but do not save as a new repo.
- DONE:  `/git remote add [remote=origin] <url>` Add a new remote repository with the specified name and URL.
- DONE: @`/git pull  [remote=origin] [branch=main] [default|ff-only|rebase|no-rebase]` Fetch and merge changes from the specified remote repository and branch into the current branch. `default` is the same as `ff-only` but included for autocomplete user-friendliness.
- DONE:  `/git fetch [remote=origin] [branch=main] [default|ff-only|rebase|no-rebase]` `/git pull`
- DONE:  `/git push  [remote=origin] [branch=main] [force|noforce]` Push committed changes to the specified remote repository and branch.
- DONE:  `/git auth <username> [password]` Store authentication credentials for accessing remote repositories. If password is omitted, only username is stored.




features I don't want to add:
      -   `/git branch [branch-name]` List all branches or switch to the specified branch.
      - #`/git add/rm @selector` Add entity(ies) to the staging area. NOTE: Selector handling (finding out which entities match something like `@e[type=...]` needs to be done by issuing commands, as this is a clientside mod)