package models

import models.Component.ComponentType.ComponentType

/**
 * Transfers are between components
 * Created by nnovod on 11/27/14.
 */
object Transfers {
	// Rules for a transfer - given two components a rule says that the transfer is (in)valid
	type TransferRules = (Component,Component) => Boolean

	/**
	 * Definition for an action
	 * @param id Identifying ID
	 * @param from component type being transferred from
	 * @param to component type being transferred to
	 * @param rules rules for this type of transfer
	 */
	case class ActionDefinition(id: Int, from: ComponentType, to: ComponentType,rules: List[TransferRules])

	/**
	 * Action that has occured
	 * @param definition id for action definition
	 */
	case class Action(definition: Int)

	/**
	 * Edge for transfer graph
	 * @param from ID of component transfer was from
	 * @param to ID of component transfer was to
	 * @param actions actions taken during transfer
	 */
	case class Edge(from: Int, to: Int, actions: List[Action])
}
