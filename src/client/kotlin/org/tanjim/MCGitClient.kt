package org.tanjim

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.command.argument.CoordinateArgument
import net.minecraft.command.argument.DefaultPosArgument
import net.minecraft.command.argument.PosArgument
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import org.tanjim.CommandQueue
class MCGitClient : ClientModInitializer {
	override fun onInitializeClient() {
		CommandQueue.init()
		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			dispatcher.register(literal("git")
				.then(literal("init")
					.then(argument<String>("name", StringArgumentType.word())
						.executes { context ->
							val name = StringArgumentType.getString(context, "name")
							context.source.sendFeedback(Text.literal(GitManager.initialize(name)))
							1
						}
					)
				)
				.then(literal("add")
					.then(argument<PosArgument>("coords1", BlockPosArgumentType.blockPos())
						.executes { context ->
							val pos1 = getClientBlockPos(context, "coords1")
							context.source.sendFeedback(Text.literal(GitManager.addBlock(pos1)))
							1
						}
						.then(argument<PosArgument>("coords2", BlockPosArgumentType.blockPos())
							.executes { context ->
								val pos1 = getClientBlockPos(context, "coords1")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Text.literal(GitManager.addBlocks(pos1, pos2, "all")))
								1
							}
							.then(literal("hollow").executes { context ->
								val pos1 = getClientBlockPos(context, "coords1")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Text.literal(GitManager.addBlocks(pos1, pos2, "hollow")))
								1
							})
							.then(literal("outline").executes { context ->
								val pos1 = getClientBlockPos(context, "coords1")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Text.literal(GitManager.addBlocks(pos1, pos2, "outline")))
								1
							})
						)
					)
				)
				.then(literal("commit")
					.then(argument<String>("message", StringArgumentType.greedyString())
						.executes { context ->
							val message = StringArgumentType.getString(context, "message")
							context.source.sendFeedback(Text.literal(GitManager.commit(message)))
							1
						}
					)
				)
				.then(literal("origin") //git origin <coords> -> GitManager.relocateOrigin
					.then(argument<PosArgument>("coords", BlockPosArgumentType.blockPos())
						.executes { context ->
							val newOrigin = getClientBlockPos(context, "coords")
							context.source.sendFeedback(Text.literal(GitManager.relocateOrigin(newOrigin)))
							1
						}
					)
				)
				.then(literal("activate") //git activate <name> -> GitManager.activateRepository
					.then(argument<String>("name", StringArgumentType.word())
						.executes { context ->
							val name = StringArgumentType.getString(context, "name")
							context.source.sendFeedback(Text.literal(GitManager.activateRepository(name)))
							1
						}
					)
				)
				.then(literal("rm") //git rm <coords> -> GitManager.rmBlock
					.then(argument<PosArgument>("coords", BlockPosArgumentType.blockPos())
						.executes { context ->
							val pos = getClientBlockPos(context, "coords")
							context.source.sendFeedback(Text.literal(GitManager.rmBlock(pos)))
							1
						}
						//git rm <coords1> <coords2> ["hollow"|"outline"|""]-> GitManager.rmBlocks
						.then(argument<PosArgument>("coords2", BlockPosArgumentType.blockPos())
							.executes { context ->
								val pos1 = getClientBlockPos(context, "coords")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Text.literal(GitManager.rmBlocks(pos1, pos2, "")))
								1
							}
							.then(literal("hollow").executes { context ->
								val pos1 = getClientBlockPos(context, "coords")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Text.literal(GitManager.rmBlocks(pos1, pos2, "hollow")))
								1
							})
							.then(literal("outline").executes { context ->
								val pos1 = getClientBlockPos(context, "coords")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Text.literal(GitManager.rmBlocks(pos1, pos2, "outline")))
								1
							})
						)
					)
				).then(literal("unstage") //git rm <coords> -> GitManager.rmBlock
					.then(argument<PosArgument>("coords", BlockPosArgumentType.blockPos())
						.executes { context ->
							val pos = getClientBlockPos(context, "coords")
							context.source.sendFeedback(Text.literal(GitManager.unstageBlock(pos)))
							1
						}
						//git rm <coords1> <coords2> ["hollow"|"outline"|""]-> GitManager.rmBlocks
						.then(argument<PosArgument>("coords2", BlockPosArgumentType.blockPos())
							.executes { context ->
								val pos1 = getClientBlockPos(context, "coords")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Text.literal(GitManager.unstageBlocks(pos1, pos2, "")))
								1
							}
							.then(literal("hollow").executes { context ->
								val pos1 = getClientBlockPos(context, "coords")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Text.literal(GitManager.unstageBlocks(pos1, pos2, "hollow")))
								1
							})
							.then(literal("outline").executes { context ->
								val pos1 = getClientBlockPos(context, "coords")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Text.literal(GitManager.unstageBlocks(pos1, pos2, "outline")))
								1
							})
						)
					)
				).then(literal("status").executes {context ->
					context.source.sendFeedback(Text.literal(GitManager.status()))
					1
				}
				).then(literal("repoList").executes { context ->
					context.source.sendFeedback(Text.literal(GitManager.listRepos()))
					1
				}
				).then(literal("commitList").executes { context ->
					context.source.sendFeedback(Text.literal(GitManager.listCommits()))
					1
				}
				).then(literal("revert")
					.executes { context ->
						context.source.sendFeedback(Text.literal(GitManager.revert()))
						1
					}
					.then(argument<String>("commitHash", StringArgumentType.word())
						.executes { context ->
							val commitHash = StringArgumentType.getString(context, "commitHash")
							context.source.sendFeedback(Text.literal(GitManager.revert(commitHash)))
							1
						}
					)
				).then(literal("reset")
					.executes { context ->
						context.source.sendFeedback(Text.literal(GitManager.reset()))
						1
					}
					.then(argument<String>("commitHash", StringArgumentType.word())
						.executes { context ->
							val commitHash = StringArgumentType.getString(context, "commitHash")
							context.source.sendFeedback(Text.literal(GitManager.reset(commitHash)))
							1
						}
					)
				).then(literal("autoadd") //git autoadd [true/false/toggle/""] -> GitManager.setAutoAdd
					.executes { context ->
						context.source.sendFeedback(Text.literal(GitManager.setAutoAdd("")))
						1
					}
					.then(literal("true").executes { context ->
						context.source.sendFeedback(Text.literal(GitManager.setAutoAdd("true")))
						1
					}
					).then(literal("false").executes { context ->
						context.source.sendFeedback(Text.literal(GitManager.setAutoAdd("false")))
						1
					}
					).then(literal("toggle").executes { context ->
						context.source.sendFeedback(Text.literal(GitManager.setAutoAdd("toggle")))
						1
					}
					)
				).then(literal("autorm") //git autorm [true/false/toggle/""] -> GitManager.setAutoRm
					.executes { context ->
						context.source.sendFeedback(Text.literal(GitManager.setAutoRm("")))
						1
					}
					.then(literal("true").executes { context ->
						context.source.sendFeedback(Text.literal(GitManager.setAutoRm("true")))
						1
					}
					).then(literal("false").executes { context ->
						context.source.sendFeedback(Text.literal(GitManager.setAutoRm("false")))
						1
					}
					).then(literal("toggle").executes { context ->
						context.source.sendFeedback(Text.literal(GitManager.setAutoRm("toggle")))
						1
					}
					)
				) //git clone <url> <name> -> GitManager.clone
				.then(literal("clone")
						.then(argument<String>("name", StringArgumentType.word())
							.then(argument<String>("url", StringArgumentType.greedyString())
							.executes { context ->
								val url = StringArgumentType.getString(context, "url")
								val name = StringArgumentType.getString(context, "name")
								context.source.sendFeedback(Text.literal(GitManager.clone(url, name)))
								1
							}
						)
					)
				) //git clonesoft <name> <url> -> GitManager.clonesoft
				.then(literal("clonesoft")
					.then(argument<String>("name", StringArgumentType.word())
						.then(argument<String>("url", StringArgumentType.greedyString())
							.executes { context ->
								val url = StringArgumentType.getString(context, "url")
								val name = StringArgumentType.getString(context, "name")
								context.source.sendFeedback(Text.literal(GitManager.clonesoft(url, name)))
								1
							}
						)
					)
				) //git put <name> -> GitManager.put
				.then(literal("put")
					.then(argument<String>("name", StringArgumentType.word())
						.executes { context ->
							val name = StringArgumentType.getString(context, "name")
							context.source.sendFeedback(Text.literal(GitManager.put(name)))
							1
						}
					)
				) //git pull [remote=origin] [branch=current] [default|ff-only|rebase|no-rebase] -> GitManager.pullRepo
				.then(literal("pull")
					.executes { context ->
						context.source.sendFeedback(Text.literal(GitManager.pullRepo("origin", null, "default")))
						1
					}
					.then(argument<String>("remote", StringArgumentType.word())
						.executes { context ->
							val remote = StringArgumentType.getString(context, "remote")
							context.source.sendFeedback(Text.literal(GitManager.pullRepo(remote, null, "default")))
							1
						}
						.then(argument<String>("branch", StringArgumentType.word())
							.executes { context ->
								val remote = StringArgumentType.getString(context, "remote")
								val branch = StringArgumentType.getString(context, "branch")
								context.source.sendFeedback(Text.literal(GitManager.pullRepo(remote, branch, "default")))
								1
							}
							.then(argument<String>("strategy", StringArgumentType.word())
								.executes { context ->
									val remote = StringArgumentType.getString(context, "remote")
									val branch = StringArgumentType.getString(context, "branch")
									val strategy = StringArgumentType.getString(context, "strategy")
									context.source.sendFeedback(
										Text.literal(
											GitManager.pullRepo(
												remote,
												branch,
												strategy
											)
										)
									)
									1
								}
							)
						)
					)
				)//git fetch [remote=origin] -> GitManger.fetch
				.then(literal("fetch")
					.executes { context ->
						context.source.sendFeedback(Text.literal(GitManager.fetch("origin")))
						1
					}
					.then(argument<String>("remote", StringArgumentType.word())
						.executes { context ->
							val remote = StringArgumentType.getString(context, "remote")
							context.source.sendFeedback(Text.literal(GitManager.fetch(remote)))
							1
						}
					)
				)//`/git push [force|noforce] [remote=origin] [branch=current]` -> GitManager.push(remote,branch,true/false)
				.then(literal("push")
					.executes{ context ->
						context.source.sendFeedback(Text.literal(GitManager.push("origin",null,false)))
						1
					}
					.then(literal("force")
						.executes{ context ->
							context.source.sendFeedback(Text.literal(GitManager.push("origin",null,true)))
							1
						}
						.then(argument<String>("remote", StringArgumentType.word())
							.executes{ context ->
								val remote=StringArgumentType.getString(context,"remote")
								context.source.sendFeedback(Text.literal(GitManager.push(remote,null,true)))
								1
							}
							.then(argument<String>("branch", StringArgumentType.word())
								.executes{ context ->
									val remote=StringArgumentType.getString(context,"remote")
									val branch=StringArgumentType.getString(context,"branch")
									context.source.sendFeedback(Text.literal(GitManager.push(remote,branch,true)))
									1
								}
							)
						)
					)
					.then(literal("noforce")
						.executes{ context ->
							context.source.sendFeedback(Text.literal(GitManager.push("origin",null,false)))
							1
						}
						.then(argument<String>("remote", StringArgumentType.word())
							.executes{ context ->
								val remote=StringArgumentType.getString(context,"remote")
								context.source.sendFeedback(Text.literal(GitManager.push(remote,null,false)))
								1
							}
							.then(argument<String>("branch", StringArgumentType.word())
								.executes{ context ->
									val remote=StringArgumentType.getString(context,"remote")
									val branch=StringArgumentType.getString(context,"branch")
									context.source.sendFeedback(Text.literal(GitManager.push(remote,branch,false)))
									1
								}
							)
						)
					)
				)//git branch <name>
				.then(literal("branch")
					.then(argument<String>("name", StringArgumentType.word())
						.executes { context ->
							val name = StringArgumentType.getString(context, "name")
							context.source.sendFeedback(Text.literal(GitManager.switchBranch(name)))
							1
						}
					)
				)//git remote add <name> <url> -> GitManager.addRemote
				.then(literal("remote")
					.then(literal("add")
						.then(argument<String>("name", StringArgumentType.word())
							.then(argument<String>("url", StringArgumentType.greedyString())
								.executes { context ->
									val name = StringArgumentType.getString(context, "name")
									val url = StringArgumentType.getString(context, "url")
									context.source.sendFeedback(Text.literal(GitManager.addRemote(url, name)))
									1
								}
							)
						)
					)
				)//git auth <username> [password] -> GitManager.setAuth
				.then(literal("auth")
					.then(argument<String>("username", StringArgumentType.word())
						.executes { context ->
							val username = StringArgumentType.getString(context, "username")
							context.source.sendFeedback(Text.literal(GitManager.setAuth(username)))
							1
						}
						.then(argument<String>("password", StringArgumentType.greedyString())
							.executes { context ->
								val username = StringArgumentType.getString(context, "username")
								val password = StringArgumentType.getString(context, "password")
								context.source.sendFeedback(Text.literal(GitManager.setAuth(username, password)))
								1
							}
						)
					)
				)
			)
		}





		UseBlockCallback.EVENT.register{ player, world, hand, hitResult ->
			if(world.isClient){
				val blockPos = hitResult.blockPos.offset(hitResult.side)
				GitManager.handleBlockPlace(blockPos)
			}
			ActionResult.PASS
		}
		AttackBlockCallback.EVENT.register{ player, world, hand, blockPos, direction ->
			if(world.isClient){
				GitManager.handleBlockBreak(blockPos)
			}
			ActionResult.PASS
		}
	}

	private fun getClientBlockPos(context: CommandContext<FabricClientCommandSource>, name: String): BlockPos {
		val arg = context.getArgument(name, PosArgument::class.java)
		val player = context.source.player ?: throw IllegalStateException("Player is null")
		if (arg is DefaultPosArgument) {
			try {
				val clazz = DefaultPosArgument::class.java
				val fields = clazz.declaredFields
				if (fields.size < 3) {
					throw IllegalStateException("DefaultPosArgument has fewer fields than expected. Cannot parse.")
				}
				val xField = fields[0]; xField.isAccessible = true
				val yField = fields[1]; yField.isAccessible = true
				val zField = fields[2]; zField.isAccessible = true
				val xArg = xField.get(arg) as CoordinateArgument
				val yArg = yField.get(arg) as CoordinateArgument
				val zArg = zField.get(arg) as CoordinateArgument
				val x = xArg.toAbsoluteCoordinate(player.x)
				val y = yArg.toAbsoluteCoordinate(player.y)
				val z = zArg.toAbsoluteCoordinate(player.z)
				return BlockPos(x.toInt(), y.toInt(), z.toInt())
			} catch (e: Exception) {
				throw RuntimeException("Failed to parse coordinates via reflection: ${e.message}")
			}
		}
		throw IllegalArgumentException("Unsupported coordinate argument type: ${arg::class.simpleName}. Only standard coords (10 64 10) or relative (~ ~ ~) are supported.")
	}
}