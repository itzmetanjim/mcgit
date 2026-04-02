package org.tanjim

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.network.chat.Component
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionResult
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
							context.source.sendFeedback(Component.literal(GitManager.initialize(name)))
							1
						}
					)
				)
				.then(literal("add")
					.then(argument("coords1", BlockPosArgument.blockPos())
						.executes { context ->
							val pos1 = getClientBlockPos(context, "coords1")
							context.source.sendFeedback(Component.literal(GitManager.addBlock(pos1)))
							1
						}
						.then(argument("coords2", BlockPosArgument.blockPos())
							.executes { context ->
								val pos1 = getClientBlockPos(context, "coords1")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Component.literal(GitManager.addBlocks(pos1, pos2, "all")))
								1
							}
							.then(literal("hollow").executes { context ->
								val pos1 = getClientBlockPos(context, "coords1")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Component.literal(GitManager.addBlocks(pos1, pos2, "hollow")))
								1
							})
							.then(literal("outline").executes { context ->
								val pos1 = getClientBlockPos(context, "coords1")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Component.literal(GitManager.addBlocks(pos1, pos2, "outline")))
								1
							})
						)
					)
				)
				.then(literal("commit")
					.then(argument<String>("message", StringArgumentType.greedyString())
						.executes { context ->
							val message = StringArgumentType.getString(context, "message")
							context.source.sendFeedback(Component.literal(GitManager.commit(message)))
							1
						}
					)
				)
				.then(literal("origin")
					.then(argument("coords", BlockPosArgument.blockPos())
						.executes { context ->
							val newOrigin = getClientBlockPos(context, "coords")
							context.source.sendFeedback(Component.literal(GitManager.relocateOrigin(newOrigin)))
							1
						}
					)
				)
				.then(literal("activate")
					.then(argument<String>("name", StringArgumentType.word())
						.executes { context ->
							val name = StringArgumentType.getString(context, "name")
							context.source.sendFeedback(Component.literal(GitManager.activateRepository(name)))
							1
						}
					)
				)
				.then(literal("rm")
					.then(argument("coords", BlockPosArgument.blockPos())
						.executes { context ->
							val pos = getClientBlockPos(context, "coords")
							context.source.sendFeedback(Component.literal(GitManager.rmBlock(pos)))
							1
						}
						.then(argument("coords2", BlockPosArgument.blockPos())
							.executes { context ->
								val pos1 = getClientBlockPos(context, "coords")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Component.literal(GitManager.rmBlocks(pos1, pos2, "")))
								1
							}
							.then(literal("hollow").executes { context ->
								val pos1 = getClientBlockPos(context, "coords")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Component.literal(GitManager.rmBlocks(pos1, pos2, "hollow")))
								1
							})
							.then(literal("outline").executes { context ->
								val pos1 = getClientBlockPos(context, "coords")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Component.literal(GitManager.rmBlocks(pos1, pos2, "outline")))
								1
							})
						)
					)
				).then(literal("unstage")
					.then(argument("coords", BlockPosArgument.blockPos())
						.executes { context ->
							val pos = getClientBlockPos(context, "coords")
							context.source.sendFeedback(Component.literal(GitManager.unstageBlock(pos)))
							1
						}
						.then(argument("coords2", BlockPosArgument.blockPos())
							.executes { context ->
								val pos1 = getClientBlockPos(context, "coords")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Component.literal(GitManager.unstageBlocks(pos1, pos2, "")))
								1
							}
							.then(literal("hollow").executes { context ->
								val pos1 = getClientBlockPos(context, "coords")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Component.literal(GitManager.unstageBlocks(pos1, pos2, "hollow")))
								1
							})
							.then(literal("outline").executes { context ->
								val pos1 = getClientBlockPos(context, "coords")
								val pos2 = getClientBlockPos(context, "coords2")
								context.source.sendFeedback(Component.literal(GitManager.unstageBlocks(pos1, pos2, "outline")))
								1
							})
						)
					)
				).then(literal("status").executes {context ->
					context.source.sendFeedback(Component.literal(GitManager.status()))
					1
				}
				).then(literal("repoList").executes { context ->
					context.source.sendFeedback(Component.literal(GitManager.listRepos()))
					1
				}
				).then(literal("commitList").executes { context ->
					context.source.sendFeedback(Component.literal(GitManager.listCommits()))
					1
				}
				).then(literal("revert")
					.executes { context ->
						context.source.sendFeedback(Component.literal(GitManager.revert()))
						1
					}
					.then(argument<String>("commitHash", StringArgumentType.word())
						.executes { context ->
							val commitHash = StringArgumentType.getString(context, "commitHash")
							context.source.sendFeedback(Component.literal(GitManager.revert(commitHash)))
							1
						}
					)
				).then(literal("reset")
					.executes { context ->
						context.source.sendFeedback(Component.literal(GitManager.reset()))
						1
					}
					.then(argument<String>("commitHash", StringArgumentType.word())
						.executes { context ->
							val commitHash = StringArgumentType.getString(context, "commitHash")
							context.source.sendFeedback(Component.literal(GitManager.reset(commitHash)))
							1
						}
					)
				).then(literal("autoadd")
					.executes { context ->
						context.source.sendFeedback(Component.literal(GitManager.setAutoAdd("")))
						1
					}
					.then(literal("true").executes { context ->
						context.source.sendFeedback(Component.literal(GitManager.setAutoAdd("true")))
						1
					}
					).then(literal("false").executes { context ->
						context.source.sendFeedback(Component.literal(GitManager.setAutoAdd("false")))
						1
					}
					).then(literal("toggle").executes { context ->
						context.source.sendFeedback(Component.literal(GitManager.setAutoAdd("toggle")))
						1
					}
					)
				).then(literal("autorm")
					.executes { context ->
						context.source.sendFeedback(Component.literal(GitManager.setAutoRm("")))
						1
					}
					.then(literal("true").executes { context ->
						context.source.sendFeedback(Component.literal(GitManager.setAutoRm("true")))
						1
					}
					).then(literal("false").executes { context ->
						context.source.sendFeedback(Component.literal(GitManager.setAutoRm("false")))
						1
					}
					).then(literal("toggle").executes { context ->
						context.source.sendFeedback(Component.literal(GitManager.setAutoRm("toggle")))
						1
					}
					)
				)
				.then(literal("clone")
						.then(argument<String>("name", StringArgumentType.word())
							.then(argument<String>("url", StringArgumentType.greedyString())
							.executes { context ->
								val url = StringArgumentType.getString(context, "url")
								val name = StringArgumentType.getString(context, "name")
								context.source.sendFeedback(Component.literal(GitManager.clone(url, name)))
								1
							}
						)
					)
				)
				.then(literal("clonesoft")
					.then(argument<String>("name", StringArgumentType.word())
						.then(argument<String>("url", StringArgumentType.greedyString())
							.executes { context ->
								val url = StringArgumentType.getString(context, "url")
								val name = StringArgumentType.getString(context, "name")
								context.source.sendFeedback(Component.literal(GitManager.clonesoft(url, name)))
								1
							}
						)
					)
				)
				.then(literal("put")
					.then(argument<String>("name", StringArgumentType.word())
						.executes { context ->
							val name = StringArgumentType.getString(context, "name")
							context.source.sendFeedback(Component.literal(GitManager.put(name)))
							1
						}
					)
				)
				.then(literal("pull")
					.executes { context ->
						context.source.sendFeedback(Component.literal(GitManager.pullRepo("origin", null, "default")))
						1
					}
					.then(argument<String>("remote", StringArgumentType.word())
						.executes { context ->
							val remote = StringArgumentType.getString(context, "remote")
							context.source.sendFeedback(Component.literal(GitManager.pullRepo(remote, null, "default")))
							1
						}
						.then(argument<String>("branch", StringArgumentType.word())
							.executes { context ->
								val remote = StringArgumentType.getString(context, "remote")
								val branch = StringArgumentType.getString(context, "branch")
								context.source.sendFeedback(Component.literal(GitManager.pullRepo(remote, branch, "default")))
								1
							}
							.then(argument<String>("strategy", StringArgumentType.word())
								.executes { context ->
									val remote = StringArgumentType.getString(context, "remote")
									val branch = StringArgumentType.getString(context, "branch")
									val strategy = StringArgumentType.getString(context, "strategy")
									context.source.sendFeedback(
										Component.literal(
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
				)
				.then(literal("fetch")
					.executes { context ->
						context.source.sendFeedback(Component.literal(GitManager.fetch("origin")))
						1
					}
					.then(argument<String>("remote", StringArgumentType.word())
						.executes { context ->
							val remote = StringArgumentType.getString(context, "remote")
							context.source.sendFeedback(Component.literal(GitManager.fetch(remote)))
							1
						}
					)
				)
				.then(literal("push")
					.executes{ context ->
						context.source.sendFeedback(Component.literal(GitManager.push("origin",null,false)))
						1
					}
					.then(literal("force")
						.executes{ context ->
							context.source.sendFeedback(Component.literal(GitManager.push("origin",null,true)))
							1
						}
						.then(argument<String>("remote", StringArgumentType.word())
							.executes{ context ->
								val remote=StringArgumentType.getString(context,"remote")
								context.source.sendFeedback(Component.literal(GitManager.push(remote,null,true)))
								1
							}
							.then(argument<String>("branch", StringArgumentType.word())
								.executes{ context ->
									val remote=StringArgumentType.getString(context,"remote")
									val branch=StringArgumentType.getString(context,"branch")
									context.source.sendFeedback(Component.literal(GitManager.push(remote,branch,true)))
									1
								}
							)
						)
					)
					.then(literal("noforce")
						.executes{ context ->
							context.source.sendFeedback(Component.literal(GitManager.push("origin",null,false)))
							1
						}
						.then(argument<String>("remote", StringArgumentType.word())
							.executes{ context ->
								val remote=StringArgumentType.getString(context,"remote")
								context.source.sendFeedback(Component.literal(GitManager.push(remote,null,false)))
								1
							}
							.then(argument<String>("branch", StringArgumentType.word())
								.executes{ context ->
									val remote=StringArgumentType.getString(context,"remote")
									val branch=StringArgumentType.getString(context,"branch")
									context.source.sendFeedback(Component.literal(GitManager.push(remote,branch,false)))
									1
								}
							)
						)
					)
				)
				.then(literal("branch")
					.then(argument<String>("name", StringArgumentType.word())
						.executes { context ->
							val name = StringArgumentType.getString(context, "name")
							context.source.sendFeedback(Component.literal(GitManager.switchBranch(name)))
							1
						}
					)
				)
				.then(literal("remote")
					.then(literal("add")
						.then(argument<String>("name", StringArgumentType.word())
							.then(argument<String>("url", StringArgumentType.greedyString())
								.executes { context ->
									val name = StringArgumentType.getString(context, "name")
									val url = StringArgumentType.getString(context, "url")
									context.source.sendFeedback(Component.literal(GitManager.addRemote(url, name)))
									1
								}
							)
						)
					)
				)
				.then(literal("auth")
					.then(argument("username", StringArgumentType.word())
						.executes { context: CommandContext<FabricClientCommandSource> ->
							val username = StringArgumentType.getString(context, "username")
							context.source.sendFeedback(Component.literal(GitManager.setAuth(username)))
							1
						}
						.then(argument("password", StringArgumentType.greedyString())
							.executes { context: CommandContext<FabricClientCommandSource> ->
								val username = StringArgumentType.getString(context, "username")
								val password = StringArgumentType.getString(context, "password")
								context.source.sendFeedback(Component.literal(GitManager.setAuth(username, password)))
								1
							}
						)
					)
				)
			)
		}




		UseBlockCallback.EVENT.register{ player, world, hand, hitResult ->
			if(world.isClientSide){
				val blockPos = hitResult.blockPos.relative(hitResult.direction)
				GitManager.handleBlockPlace(blockPos)
			}
			InteractionResult.PASS
		}
		AttackBlockCallback.EVENT.register{ player, world, hand, blockPos, direction ->
			if(world.isClientSide){
				GitManager.handleBlockBreak(blockPos)
			}
			InteractionResult.PASS
		}
	}
	
	companion object {
		private fun getClientBlockPos(context: CommandContext<FabricClientCommandSource>, argName: String): BlockPos {
			val coords = context.getArgument(argName, Coordinates::class.java)
			val source = context.source
			val playerPos = source.getPosition()
			
			val x = getCoordinate(coords, playerPos.x, coords.isXRelative, "x", "left")
			val y = getCoordinate(coords, playerPos.y, coords.isYRelative, "y", "up")
			val z = getCoordinate(coords, playerPos.z, coords.isZRelative, "z", "forwards")
			
			return BlockPos(x.toInt(), y.toInt(), z.toInt())
		}
		
		private fun getCoordinate(coords: Coordinates, playerValue: Double, isRelative: Boolean, absoluteField: String, relativeField: String): Double {
			return try {
				val field = coords.javaClass.getDeclaredField(if (isRelative) relativeField else absoluteField)
				field.isAccessible = true
				val value = field.getDouble(coords)
				if (isRelative) playerValue + value else value
			} catch (e: Exception) {
				playerValue
			}
		}
	}
}