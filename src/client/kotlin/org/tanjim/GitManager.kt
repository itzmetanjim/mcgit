package org.tanjim

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.nbt.NbtHelper
import net.minecraft.util.math.BlockPos
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.lib.ObjectId
import java.io.File
import net.minecraft.nbt.NbtCompound

object GitManager {
    private val gameDir: File = FabricLoader.getInstance().gameDir.toFile()
    private val rootReposDir = File(gameDir, "mcgit_repos")
    private val autoAddQueue = mutableSetOf<BlockPos>()
    var activeRepo: String?
        get() {
            val file = File(rootReposDir, "active_repo.txt")
            return if (file.exists()) file.readText().trim() else null
        }
        set(value) {
            rootReposDir.mkdirs()
            val file = File(rootReposDir, "active_repo.txt")
            if (value == null) {
                if (file.exists()) file.delete()
            } else {
                file.writeText(value)
            }
        }
    var shouldAutoAdd: Boolean
        get(){
            return !File(rootReposDir, "no_auto_add.txt").exists()
        }
        set(value){
            rootReposDir.mkdirs()
            val file = File(rootReposDir, "no_auto_add.txt")
            if (!value){
                if (!file.exists()) file.writeText("no_auto_add")
            } else {
                if (file.exists()) file.delete()
            }
        }
    var shouldAutoRm: Boolean
        get(){
            return !File(rootReposDir, "no_auto_rm.txt").exists()
        }
        set(value){
            rootReposDir.mkdirs()
            val file = File(rootReposDir, "no_auto_rm.txt")
            if (!value){
                if (!file.exists()) file.writeText("no_auto_rm")
            } else {
                if (file.exists()) file.delete()
            }
        }

    // Authentication storage
    private fun getAuthFile(): File {
        rootReposDir.mkdirs()
        return File(rootReposDir, "auth.txt")
    }

    private fun getCredentialsProvider(): UsernamePasswordCredentialsProvider? {
        val authFile = getAuthFile()
        if (!authFile.exists()) return null
        return try {
            val lines = authFile.readLines()
            if (lines.isEmpty()) return null
            val username = lines[0]
            val password = if (lines.size > 1) lines[1] else ""
            UsernamePasswordCredentialsProvider(username, password)
        } catch (_: Exception) {
            null
        }
    }

    fun setAuth(username: String, password: String? = null): String {
        return try {
            val authFile = getAuthFile()
            if (password != null) {
                authFile.writeText("$username\n$password")
                "Authentication credentials stored for user '$username'."
            } else {
                authFile.writeText(username)
                "Username '$username' stored (no password)."
            }
        } catch (e: Exception) {
            "Error storing authentication: ${e.message}"
        }
    }

    fun getRepoRoot(): File? {
        val name = activeRepo ?: return null
        return File(rootReposDir, name)
    }
    fun getActiveDimensionDir(): File? {
        val root = getRepoRoot() ?: return null
        val world = MinecraftClient.getInstance().world ?: return null
        val dim = world.registryKey.value
        return File(root, "${dim.namespace}/${dim.path}")
    }
    fun activateRepository(name: String): String{
        val repoRoot = File(rootReposDir, name)
        if (!repoRoot.exists() || !File(repoRoot, ".git").exists()) {
            return "Error: Repository '$name' does not exist."
        }
        activeRepo = name
        return "Activated repository '$name'."
    }
    private fun getCurrentDimensionPath(): String {
        val world = MinecraftClient.getInstance().world ?: return "overworld"
        val id = world.registryKey.value
        return "${id.namespace}/${id.path}"
    }
    fun getOrigin(): BlockPos {
        val root = getRepoRoot() ?: return BlockPos.ORIGIN
        val file = File(root, "origin.txt")
        if (!file.exists()) return BlockPos.ORIGIN

        return try {
            val parts = file.readText().trim().split(",")
            BlockPos(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        } catch (_: Exception) {
            BlockPos.ORIGIN
        }
    }

    fun saveOrigin(pos: BlockPos) {
        val root = getRepoRoot() ?: return
        root.mkdirs()
        File(root, "origin.txt").writeText("${pos.x},${pos.y},${pos.z}")
    }

    private fun getCurrentBranch(): String {
        val root = getRepoRoot() ?: return "main"
        return try {
            Git.open(root).use { git ->
                git.repository.branch ?: "main"
            }
        } catch (_: Exception) {
            "main"
        }
    }

    fun initialize(name: String): String {
        if (name.isBlank()) {
            return "Error: Repository name is required."
        }
        val repoRoot = File(rootReposDir, name)

        if (!repoRoot.exists()) repoRoot.mkdirs()

        return try {
            Git.init().setDirectory(repoRoot).setInitialBranch("main").call().use {
                activeRepo = name
                val player = MinecraftClient.getInstance().player
                if (player != null) saveOrigin(player.blockPos)
                "Initialized new MCGit repository '$name' at ${repoRoot.absolutePath}"
            }
        } catch (e: Exception) {
            "Error in initializing: ${e.message}"
        }
    }

    private fun saveBlockToDisk(pos: BlockPos): Boolean {
        val world = MinecraftClient.getInstance().world ?: return false
        val dimDir = getActiveDimensionDir() ?: return false
        val origin = getOrigin()

        val relX = pos.x - origin.x
        val relY = pos.y - origin.y
        val relZ = pos.z - origin.z

        val state = world.getBlockState(pos)
        val snbt = NbtHelper.fromBlockState(state).toString()

        val file = File(dimDir, "$relX/$relY/$relZ.snbt")
        return try {
            file.parentFile?.mkdirs()
            file.writeText(snbt)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun addBlock(pos: BlockPos): String {
        if (!saveBlockToDisk(pos)) return "Error: Could not save block."

        return try {
            val root = getRepoRoot() ?: return "Error"
            Git.open(root).use { git ->
                git.add().addFilepattern(getCurrentDimensionPath()).call()
                "Added block at ${pos.toShortString()}. Use /git unstage to undo."
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun addBlocks(pos1: BlockPos, pos2: BlockPos, option: String): String {
        val root = getRepoRoot() ?: return "Error"
        var count = 0

        val minX = minOf(pos1.x, pos2.x); val maxX = maxOf(pos1.x, pos2.x)
        val minY = minOf(pos1.y, pos2.y); val maxY = maxOf(pos1.y, pos2.y)
        val minZ = minOf(pos1.z, pos2.z); val maxZ = maxOf(pos1.z, pos2.z)

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val isEdge = (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ)
                    val isCol = (x == minX || x == maxX) && (z == minZ || z == maxZ)

                    val shouldAdd = when(option.lowercase()) {
                        "hollow" -> isEdge
                        "outline" -> isCol
                        else -> true
                    }

                    if (shouldAdd && saveBlockToDisk(BlockPos(x, y, z))) count++
                }
            }
        }

        return try {
            Git.open(root).use { git ->
                git.add().addFilepattern(getCurrentDimensionPath()).call()
                "Added $count blocks to '${activeRepo}'. Use /git unstage to undo."
            }
        } catch (e: Exception) {
            "Error in adding range of blocks : ${e.message}"
        }
    }

    fun commit(message: String): String {
        val root = getRepoRoot() ?: return "Error: No active repository. Try /git init or /git activate <repo>."
        return try {
            Git.open(root).use { git ->
                val rev = git.commit().setMessage(message).call()
                val branch = git.repository.branch ?: "main"
                "[${root.name}/$branch ${rev.name.substring(0, 7)}] $message"
            }
        } catch (e: Exception) {
            "Error commiting: ${e.message}"
        }
    }

    fun relocateOrigin(newOrigin: BlockPos): String {
        val root = getRepoRoot() ?: return "Error"
        val oldOrigin = getOrigin()

        val dx = newOrigin.x - oldOrigin.x
        val dy = newOrigin.y - oldOrigin.y
        val dz = newOrigin.z - oldOrigin.z

        root.listFiles { f -> f.isDirectory && f.name != ".git" }?.forEach { dimFolder ->
            val snbtFiles = dimFolder.walkTopDown().filter { it.extension == "snbt" }.toList()

            snbtFiles.forEach { file ->
                val relPath = file.relativeTo(dimFolder).path.replace(".snbt", "")
                val parts = relPath.split(File.separator)
                if (parts.size == 3) {
                    val oldRelX = parts[0].toInt()
                    val oldRelY = parts[1].toInt()
                    val oldRelZ = parts[2].toInt()
                    val newX = oldRelX - dx
                    val newY = oldRelY - dy
                    val newZ = oldRelZ - dz

                    val newFile = File(dimFolder, "$newX/$newY/$newZ.snbt")
                    newFile.parentFile.mkdirs()
                    file.renameTo(newFile)
                }
            }
        }

        saveOrigin(newOrigin)
        return try {
            Git.open(root).use { git ->
                git.add().addFilepattern(".").call()
                val branch = git.repository.branch ?: "main"
                "[${root.name}/$branch] Origin moved to ${newOrigin.toShortString()}"
            }
        } catch (e: Exception) { "Error in setting origin: ${e.message}" }
    }
    fun rmBlock(pos: BlockPos): String {
        val root = getRepoRoot() ?: return "Error: No active repository. Try /git init or /git activate <repo>."
        val origin = getOrigin()
        val relX = pos.x - origin.x
        val relY = pos.y - origin.y
        val relZ = pos.z - origin.z

        val dimDir = getActiveDimensionDir() ?: return "Error: No active dimension. Try again later."
        val fileName = "$relX/$relY/$relZ.snbt"
        val file = File(dimDir, fileName)

        if (file.exists()) {
            file.delete()
        } else {
            return "Error: Block at ${pos.toShortString()} is not tracked."
        }

        CommandQueue.add("setblock ${pos.x} ${pos.y} ${pos.z} air")

        return try {
            Git.open(root).use { git ->
                val gitPath = "${getCurrentDimensionPath()}/$fileName"
                git.rm().addFilepattern(gitPath).call()
                "Removed block at ${pos.toShortString()}."
            }
        } catch (e: Exception) {
            "Error in removing block: ${e.message}"
        }
    }

    fun rmBlocks(pos1: BlockPos, pos2: BlockPos, option: String): String {
        val root = getRepoRoot() ?: return "Error: No active repository. Try /git init or /git activate <repo>."
        val origin = getOrigin()

        val minX = minOf(pos1.x, pos2.x); val maxX = maxOf(pos1.x, pos2.x)
        val minY = minOf(pos1.y, pos2.y); val maxY = maxOf(pos1.y, pos2.y)
        val minZ = minOf(pos1.z, pos2.z); val maxZ = maxOf(pos1.z, pos2.z)
        var count = 0

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val isEdge = (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ)
                    val isCol = (x == minX || x == maxX) && (z == minZ || z == maxZ)

                    val shouldRemove = when(option.lowercase()) {
                        "hollow" -> isEdge
                        "outline" -> isCol
                        else -> true
                    }

                    if (shouldRemove) {
                        val relX = x - origin.x
                        val relY = y - origin.y
                        val relZ = z - origin.z

                        val dimDir = getActiveDimensionDir() ?: continue
                        val file = File(dimDir, "$relX/$relY/$relZ.snbt")
                        if (file.exists()) {
                            file.delete()
                        }
                        count++
                    }
                }
            }
        }

        val cmdoption = if(option != "") option else "replace"
        CommandQueue.add("fill $minX $minY $minZ $maxX $maxY $maxZ air $cmdoption")

        try {
            Git.open(root).use { git ->
                git.add().addFilepattern(getCurrentDimensionPath()).setUpdate(true).call()
            }
            return "Removed $count blocks from '${activeRepo}'."
        } catch (e: Exception) {
            return "Error in removing range of blocks : ${e.message}"
        }
    }

    fun unstageBlock(pos: BlockPos): String {
        val root = getRepoRoot() ?: return "Error: No active repository. Try /git init or /git activate <repo>."
        val origin = getOrigin()
        val relX = pos.x - origin.x
        val relY = pos.y - origin.y
        val relZ = pos.z - origin.z

        val dimDir = getActiveDimensionDir() ?: return "Error: No active dimension. Try again later."
        val fileName = "$relX/$relY/$relZ.snbt"
        val file = File(dimDir, fileName)

        if (!file.exists()) {
            return "Error: Block at ${pos.toShortString()} is not tracked."
        }

        return try {
            Git.open(root).use { git ->
                val gitPath = "${getCurrentDimensionPath()}/$fileName"
                git.rm().addFilepattern(gitPath).call()
                "Removed block at ${pos.toShortString()}."
            }
        } catch (e: Exception) {
            "Error in unstaging block: ${e.message}"
        }
    }

    fun unstageBlocks(pos1: BlockPos, pos2: BlockPos, option: String): String {
        val root = getRepoRoot() ?: return "Error: No active repository. Try /git init or /git activate <repo>."
        val origin = getOrigin()

        val minX = minOf(pos1.x, pos2.x); val maxX = maxOf(pos1.x, pos2.x)
        val minY = minOf(pos1.y, pos2.y); val maxY = maxOf(pos1.y, pos2.y)
        val minZ = minOf(pos1.z, pos2.z); val maxZ = maxOf(pos1.z, pos2.z)

        val filesToUnstage = mutableListOf<String>()
        var count = 0

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val isEdge = (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ)
                    val isCol = (x == minX || x == maxX) && (z == minZ || z == maxZ)

                    val shouldRemove = when(option.lowercase()) {
                        "hollow" -> isEdge
                        "outline" -> isCol
                        else -> true
                    }

                    if (shouldRemove) {
                        val relX = x - origin.x
                        val relY = y - origin.y
                        val relZ = z - origin.z

                        val fileName = "$relX/$relY/$relZ.snbt"
                        val gitPath = "${getCurrentDimensionPath()}/$fileName"
                        filesToUnstage.add(gitPath)
                        count++
                    }
                }
            }
        }

        try {
            Git.open(root).use { git ->
                val rm = git.rm()
                filesToUnstage.forEach { path ->
                    rm.addFilepattern(path)
                }
                rm.call()
            }
            return "Unstaged $count blocks from '${activeRepo}'."
        } catch (e: Exception) {
            return "Error in unstaging range of blocks : ${e.message}"
        }
    }
    fun nbtToSetblock(x: Int, y:Int, z:Int, nbt: NbtCompound):String{
        val blockId=nbt.getString("Name");
        val properties=nbt.getCompound("Properties")
        val sb = StringBuilder("setblock $x $y $z ${blockId.get()}")
        properties.ifPresent{nbt->
            if(!nbt.isEmpty){
            val propList = mutableListOf<String>()
            for (key in nbt.keys) {
                val value = nbt.get(key)?.asString()?.get() ?: continue
                propList.add("$key=$value")
            }
            sb.append("[${propList.joinToString(",")}]")
        }}

        sb.append(" replace")
        return sb.toString()
    }
    fun reset( hash: String? = null):String{
        val root= getRepoRoot() ?: return "Error: No active repository. Try /git init or /git activate <repo>."
        return try {
            Git.open(root).use{git->
                if(git.repository.resolve("HEAD")==null){
                    return "Error: No commits to reset to. Try /git commit first."
                }
                val cmd=git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD)
                if (hash!=null){cmd.setRef(hash)}
                else{
                    cmd.setRef(git.repository.resolve("HEAD")?.name)
                }
                cmd.call()
                "Reset index to ${"commit $hash"?:"HEAD"}."
            }
        } catch(e: Exception){"Error in reset:${e.message}"}
    }
    private fun getWorkingTreeBBox(): BBox? {
        val origin = getOrigin()
        val dimDir = getActiveDimensionDir() ?: return null
        var minX = Int.MAX_VALUE; var minY = Int.MAX_VALUE; var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE; var maxY = Int.MIN_VALUE; var maxZ = Int.MIN_VALUE
        var found = false
        dimDir.walkTopDown().filter { it.extension == "snbt" }.forEach { file ->
            val relPath = file.relativeTo(dimDir).path.replace(".snbt", "")
            val parts = relPath.split(File.separator)
            if (parts.size == 3) {
                val wx = parts[0].toInt() + origin.x
                val wy = parts[1].toInt() + origin.y
                val wz = parts[2].toInt() + origin.z
                if (wx < minX) minX = wx; if (wx > maxX) maxX = wx
                if (wy < minY) minY = wy; if (wy > maxY) maxY = wy
                if (wz < minZ) minZ = wz; if (wz > maxZ) maxZ = wz
                found = true
            }
        }
        return if (found) BBox(minX, minY, minZ, maxX, maxY, maxZ) else null
    }

    /**
     * Returns the bounding box (as world coords) of all .snbt files in a given git commit/ref
     * for the active dimension, or null if there are none.
     */
    private fun getCommitBBox(git: Git, ref: String): BBox? {
        val origin = getOrigin()
        val dimPrefix = getCurrentDimensionPath().replace("\\", "/") + "/"
        val objectId = git.repository.resolve(ref) ?: return null
        val commit = git.repository.parseCommit(objectId)
        var minX = Int.MAX_VALUE; var minY = Int.MAX_VALUE; var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE; var maxY = Int.MIN_VALUE; var maxZ = Int.MIN_VALUE
        var found = false
        TreeWalk(git.repository).use { tw ->
            tw.addTree(commit.tree)
            tw.isRecursive = true
            while (tw.next()) {
                val path = tw.pathString
                if (path.startsWith(dimPrefix) && path.endsWith(".snbt")) {
                    val rel = path.removePrefix(dimPrefix).removeSuffix(".snbt")
                    val parts = rel.split("/")
                    if (parts.size == 3) {
                        val wx = parts[0].toInt() + origin.x
                        val wy = parts[1].toInt() + origin.y
                        val wz = parts[2].toInt() + origin.z
                        if (wx < minX) minX = wx; if (wx > maxX) maxX = wx
                        if (wy < minY) minY = wy; if (wy > maxY) maxY = wy
                        if (wz < minZ) minZ = wz; if (wz > maxZ) maxZ = wz
                        found = true
                    }
                }
            }
        }
        return if (found) BBox(minX, minY, minZ, maxX, maxY, maxZ) else null
    }

    data class BBox(val minX: Int, val minY: Int, val minZ: Int, val maxX: Int, val maxY: Int, val maxZ: Int) {
        fun union(other: BBox) = BBox(
            minOf(minX, other.minX), minOf(minY, other.minY), minOf(minZ, other.minZ),
            maxOf(maxX, other.maxX), maxOf(maxY, other.maxY), maxOf(maxZ, other.maxZ)
        )
    }

    fun gitToWorld(clearBBox: BBox? = null){
        val origin = getOrigin()
        val dimDir = getActiveDimensionDir() ?: return
        data class BlockEntry(val worldX: Int, val worldY: Int, val worldZ: Int, val snbt: String)
        val entries = mutableListOf<BlockEntry>()
        dimDir.walkTopDown().filter { it.extension == "snbt" }.forEach { file ->
            val relPath = file.relativeTo(dimDir).path.replace(".snbt", "")
            val parts = relPath.split(File.separator)
            if (parts.size == 3) {
                val worldX = parts[0].toInt() + origin.x
                val worldY = parts[1].toInt() + origin.y
                val worldZ = parts[2].toInt() + origin.z
                entries.add(BlockEntry(worldX, worldY, worldZ, file.readText()))
            }
        }
        val bbox: BBox? = clearBBox ?: if (entries.isEmpty()) null else BBox(
            entries.minOf { it.worldX }, entries.minOf { it.worldY }, entries.minOf { it.worldZ },
            entries.maxOf { it.worldX }, entries.maxOf { it.worldY }, entries.maxOf { it.worldZ }
        )

        if (bbox != null) {
            CommandQueue.add("fill ${bbox.minX} ${bbox.minY} ${bbox.minZ} ${bbox.maxX} ${bbox.maxY} ${bbox.maxZ} air replace")
        }

        for (entry in entries) {
            val nbt = net.minecraft.nbt.StringNbtReader.readCompound(entry.snbt) as NbtCompound
            val cmd = nbtToSetblock(entry.worldX, entry.worldY, entry.worldZ, nbt)
            CommandQueue.add(cmd)
        }
    }
    fun revert(hash:String? = null):String{
        val root= getRepoRoot() ?: return "Error: No active repository. Try /git init or /git activate <repo>."
        return try {
            Git.open(root).use{git->
                if(git.repository.resolve("HEAD")==null){
                    return "Error: No commits to revert to. Try /git commit first."
                }
                val targetRef = hash ?: git.repository.resolve("HEAD")?.name
                    ?: return "Error: Could not resolve HEAD."
                val preBBox = getWorkingTreeBBox()
                val postBBox = getCommitBBox(git, targetRef)
                val clearBBox: BBox? = when {
                    preBBox != null && postBBox != null -> preBBox.union(postBBox)
                    preBBox != null -> preBBox
                    postBBox != null -> postBBox
                    else -> null
                }
                val cmd = git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD)
                cmd.setRef(targetRef)
                cmd.call()
                gitToWorld(clearBBox)
                "Reverted world to ${if (hash != null) "commit $hash" else "HEAD"}."
            }
        } catch(e: Exception){"Error in revert: ${e.message}"}
    }
    fun listRepos():String{
        val sb = StringBuilder()
        val flist = rootReposDir.listFiles()
        if (flist == null || flist.isEmpty()) {
            return "No repositories found."
        }
        val active = activeRepo
        sb.append("There are ${flist.size-1} repositories:\n")
        for (file in flist) {
            if (file.isDirectory && File(file, ".git").exists()) {
                if (file.name == active) {
                    sb.append("* ${file.name}\n")
                } else {
                    sb.append("  ${file.name}\n")
                }
            }
        }
        return sb.toString().trimEnd()
    }
    fun listCommits():String{
        val root= getRepoRoot() ?: return "Error: No active repository. Try /git init or /git activate <repo>."
        return try {
            val sb = StringBuilder()
            Git.open(root).use{git->
                val log = git.log().call()
                sb.append("Commit history for repository '${root.name}':\n")
                for (commit in log) {
                    sb.append("- ${commit.name.substring(0,7)}: ${commit.shortMessage}\n")
                }
            }
            sb.toString().trimEnd()
        } catch(e: Exception){"Error in listing commits:${e.message}"}
    }
    fun status():String{
        val root = getRepoRoot() ?: return "Error: No active repository. Try /git init or /git activate <repo>."
        try {
            val sb= StringBuilder()
            Git.open(root).use{git->
                val branch = git.repository.branch ?: "main"
                sb.append("[${root.name}/$branch]\n")
                val status = git.status().call()
                if (status.hasUncommittedChanges()){
                    sb.append("Uncommitted changes:\n")
                    sb.append("+ Blocks added: ${status.added.size}\n")
                    sb.append("- Blocks removed: ${status.removed.size}\n")
                    sb.append("* Blocks modified: ${status.modified.size}")
                } else {
                    sb.append("No uncommitted changes.")
                }
            }
            return sb.toString()
        } catch (e:Exception) {return "Error in getting status: ${e.message}"} //im tired of the kotlin "return try" statements
    }
    fun isMagicOffhand(): Boolean {
        val player = MinecraftClient.getInstance().player ?: return false
        val stack = player.offHandStack
        if (stack.item != net.minecraft.item.Items.RED_WOOL) return false
        return stack.hasEnchantments()
    }
    fun setAutoAdd(option: String):String{ //option is true/false/toggle/empty string
        return try {
            when(option.lowercase()){
                "true"->{
                    shouldAutoAdd=true
                    "Auto-add enabled."
                }
                "false"->{
                    shouldAutoAdd=false
                    "Auto-add disabled."
                }
                "toggle"->{
                    shouldAutoAdd = !shouldAutoAdd
                    "Auto-add ${if (shouldAutoAdd) "enabled" else "disabled"}."
                }
                ""->{
                    "Auto-add is currently ${if (shouldAutoAdd) "enabled" else "disabled"}. Use /git autoadd <true|false|toggle> to change."
                }
                else->"Error: Invalid option. Use true/false/toggle."
            }
        } catch(e: Exception){"Error in setting auto-add: ${e.message}"}

    }
    fun setAutoRm(option: String):String{ //option is true/false/toggle/empty string
        return try {
            when(option.lowercase()){
                "true"->{
                    shouldAutoRm=true
                    "Auto-rm enabled."
                }
                "false"->{
                    shouldAutoRm=false
                    "Auto-rm disabled."
                }
                "toggle"->{
                    shouldAutoRm = !shouldAutoRm
                    "Auto-rm ${if (shouldAutoRm) "enabled" else "disabled"}."
                }
                ""->{
                    "Auto-rm is currently ${if (shouldAutoRm) "enabled" else "disabled"}. Use /git autorm <true|false|toggle> to change."
                }
                else->"Error: Invalid option. Use true/false/toggle."
            }
        } catch(e: Exception){"Error in setting auto-rm: ${e.message}"}

    }
    fun isCreative(client: MinecraftClient): Boolean {
        val mode = client.interactionManager?.currentGameMode
        return mode == net.minecraft.world.GameMode.CREATIVE
    }
    fun handleBlockPlace(pos: BlockPos){
        val client = MinecraftClient.getInstance()
        if(!isCreative(client)) return
        val hasMagicWool = isMagicOffhand()
        if (shouldAutoAdd != hasMagicWool) {
            /*val msg=addBlock(pos)
            client.player?.sendMessage(net.minecraft.text.Text.literal(msg), true)
            println("[MCGit] Auto-add: $msg")*/
            autoAddQueue.add(pos)
        }
    }
    fun handleBlockBreak(pos: BlockPos){
        val client = MinecraftClient.getInstance()
        if(!isCreative(client)) return
        val hasMagicWool = isMagicOffhand()
        if (shouldAutoRm != hasMagicWool) {
            val msg=unstageBlock(pos)
            client.player?.sendMessage(net.minecraft.text.Text.literal(msg), true)
            println("[MCGit] Auto-rm: $msg")
        }
    }
    fun processTick(){
        if(autoAddQueue.isEmpty()) return
        val client = MinecraftClient.getInstance()
        val world = client.world ?: return
        val iterator = autoAddQueue.iterator()
        while (iterator.hasNext()) {
            val pos = iterator.next()
            if(!world.getBlockState(pos).isAir){
                val msg = addBlock(pos)
                MinecraftClient.getInstance().player?.sendMessage(net.minecraft.text.Text.literal(msg), true)
                println("[MCGit] Auto-add: $msg")
                iterator.remove()
            }
        }
    }
    fun clonesoft(url:String,name:String):String{
        if(File(rootReposDir,name).exists()){
            return "Error: Target repository '$name' already exists."
        }
        if(!url.startsWith("http")){
            val src = File(rootReposDir,url)
            if (!src.exists() || !File(src, ".git").exists()) {
                return ("Error: Source repository '$url' does not exist.")
            }
            try{
                val dest = File(rootReposDir,name)
                src.copyTo(dest,true)
                activateRepository(name)
                val client = MinecraftClient.getInstance()
                val player = client.player
                if (player != null) saveOrigin(player.blockPos)
                return "Cloned local repository '$url' to '$name'."
            }catch(e:Exception){
                return "Error in cloning local repository: ${e.message}"
            }
        }
        try{
            val dest = File(rootReposDir,name)
            val cloneCmd = Git.cloneRepository().setURI(url).setDirectory(dest)
            getCredentialsProvider()?.let { cloneCmd.setCredentialsProvider(it) }
            cloneCmd.call()
            activateRepository(name)
            val client = MinecraftClient.getInstance()
            val player = client.player
            if (player != null) saveOrigin(player.blockPos)
            return "Cloned repository from '$url' to '$name'."
        }catch(e:Exception){
            return "Error in cloning repository from URL: ${e.message}"
        }
    }


    fun clone(url:String,name:String):String{
        if(File(rootReposDir,name).exists()){
            return "Error: Target repository '$name' already exists."
        }
        if(!url.startsWith("http")){
            val src = File(rootReposDir,url)
            if (!src.exists() || !File(src, ".git").exists()) {
                return ("Error: Source repository '$url' does not exist.")
            }
            try{
                val dest = File(rootReposDir,name)
                src.copyTo(dest,true)
                activateRepository(name)
                val client = MinecraftClient.getInstance()
                val player = client.player
                if (player != null) saveOrigin(player.blockPos)
                gitToWorld()
                return "Cloned local repository '$url' to '$name'."
            }catch(e:Exception){
                return "Error in cloning local repository: ${e.message}"
            }
        }
        try{
            val dest = File(rootReposDir,name)
            val cloneCmd = Git.cloneRepository().setURI(url).setDirectory(dest)
            getCredentialsProvider()?.let { cloneCmd.setCredentialsProvider(it) }
            cloneCmd.call()
            activateRepository(name)
            val client = MinecraftClient.getInstance()
            val player = client.player
            if (player != null) saveOrigin(player.blockPos)
            gitToWorld()
            return "Cloned repository from '$url' to '$name'."
        }catch(e:Exception){
            return "Error in cloning repository from URL: ${e.message}"
        }
    }

    fun put(name:String):String{
        val oldActivated = activeRepo
        activateRepository(name)
        val oldOrigin = getOrigin()
        val client = MinecraftClient.getInstance()
        val player = client.player
        if (player == null) return "Error: No player found."
        saveOrigin(player.blockPos)
        gitToWorld()
        saveOrigin(oldOrigin)
        activateRepository(oldActivated?:"")
        return "Put repository '$name' into the world at your current position."
    }
    fun addRemote(url:String,remoteName:String="origin"):String{
        val root= getRepoRoot() ?: return "Error: No active repository. Try /git init or /git activate <repo>."
        return try {
            Git.open(root).use{git->
                git.remoteAdd().setName(remoteName).setUri(org.eclipse.jgit.transport.URIish(url)).call()
                "Added remote '$remoteName' with URL '$url'."
            }
        } catch(e: Exception){"Error in adding remote:${e.message}"}
    }
    //git pull  [remote=origin] [branch=current] [default|ff-only|rebase|no-rebase]
    fun pullRepo(remote:String="origin",branch:String?=null,option:String="default"):String{
        val root= getRepoRoot() ?: return "Error: No active repository. Try /git init or /git activate <repo>."
        return try {
            Git.open(root).use{git->
                val actualBranch = branch ?: git.repository.branch ?: "main"
                val pullCmd=git.pull().setRemote(remote).setRemoteBranchName(actualBranch)
                getCredentialsProvider()?.let { pullCmd.setCredentialsProvider(it) }
                when(option.lowercase()){
                    "rebase"->pullCmd.setRebase(true)
                    "no-rebase"->pullCmd.setRebase(false)
                }
                val result=pullCmd.call()
                if (result.isSuccessful){
                    gitToWorld()
                    "Pulled from $remote/$actualBranch successfully."
                } else {
                    "Error in pulling: ${result.mergeResult?.toString()?:"Unknown error"}"
                }
            }
        } catch(e: Exception){"Error in pulling:${e.message}"}
    }
    fun fetch(remote:String="origin"):String{
        val root= getRepoRoot() ?: return "Error: No active repository. Try /git init or /git activate <repo>."
        return try {
            Git.open(root).use{git->
                val fetchCmd = git.fetch().setRemote(remote)
                getCredentialsProvider()?.let { fetchCmd.setCredentialsProvider(it) }
                fetchCmd.call()
                "Fetched from $remote successfully."
            }
        } catch(e: Exception){"Error in fetching:${e.message}"}
    }
    fun push(remote:String="origin",branch:String?=null,force:Boolean=false):String{
        val root= getRepoRoot() ?: return "Error: No active repository. Try /git init or /git activate <repo>."
        return try {
            Git.open(root).use{git->
                val actualBranch = branch ?: git.repository.branch ?: "main"
                val pushCmd=git.push().setRemote(remote).add(actualBranch)
                if (force) pushCmd.setForce(true)
                getCredentialsProvider()?.let { pushCmd.setCredentialsProvider(it) }
                val results=pushCmd.call()
                val sb=StringBuilder()
                for (result in results){
                    val messages=result.messages
                    if (messages.isNotBlank()){
                        sb.append(messages)
                    }
                }
                if (sb.isEmpty()){
                    "Pushed to $remote/$actualBranch successfully."
                } else {
                    "Push completed with messages:\n${sb.toString()}"
                }
            }
        } catch(e: Exception){"Error in pushing:${e.message}"}

    }
    fun switchBranch(branchName:String):String{
        val root= getRepoRoot() ?: return "Error: No active repository. Try /git init or /git activate <repo>."
        return try {
            Git.open(root).use{git->
                // if branch not exists create!!
                val branches=git.branchList().call().map { it.name.substringAfterLast("/") }
                if (!branches.contains(branchName)){
                    git.checkout().setCreateBranch(true).setName(branchName).call()
                }
                git.checkout().setName(branchName).call()
                gitToWorld()
                "Switched to branch '$branchName'."
            }
        } catch(e: Exception){"Error in switching branch:${e.message}"}
    }
}