package com.novalnet.novalnetordermanagement.actions.returns;

import de.hybris.platform.core.model.order.OrderModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.hybris.cockpitng.engine.impl.AbstractComponentWidgetAdapterAware;
import com.novalnet.facades.NovalnetTransactionFacade;

import jakarta.annotation.Resource;


/**
 * Backoffice action responsible for creating a return request for a Novalnet order.
 * <p>
 * Validates whether a return request can be created and sends the selected order
 * to the return request widget through the configured output socket.
 */
public class NovalnetCreateReturnRequestAction extends AbstractComponentWidgetAdapterAware
		implements CockpitAction<OrderModel, OrderModel>
{
	private static final Logger LOG = LoggerFactory.getLogger(NovalnetCreateReturnRequestAction.class);

	protected static final String SOCKET_OUT_CONTEXT = "createReturnRequestContext";

	@Resource(name = "novalnetTransactionFacade")
	private NovalnetTransactionFacade novalnetTransactionFacade;

	/**
	 * Determines whether a return request can be created for the selected order.
	 *
	 * @param actionContext the action context containing the selected order
	 * @return {@code true} if a return request can be created; otherwise {@code false}
	 */
	@Override
	public boolean canPerform(final ActionContext<OrderModel> actionContext)
	{
		final OrderModel order = actionContext.getData();

		if (order == null)
		{
			LOG.info("Order is null, return request button will not be visible");
			return false;
		}

		return novalnetTransactionFacade.canCreateReturnRequest(order);
	}

	/**
	 * Starts the return request process for the selected order and sends the order
	 * to the return request widget through the output socket.
	 *
	 * @param actionContext the action context containing the selected order
	 * @return the action result containing the selected order
	 */
	@Override
	public ActionResult<OrderModel> perform(final ActionContext<OrderModel> actionContext)
	{
		final OrderModel order = actionContext.getData();

		if (order == null)
		{
			LOG.warn("Order is null in return request action");
			return new ActionResult<>(ActionResult.ERROR, null);
		}

		LOG.info("Starting return request for order {}", order.getCode());

		sendOutput(SOCKET_OUT_CONTEXT, order);

		LOG.info("Return request completed for order {}", order.getCode());

		final ActionResult<OrderModel> result = new ActionResult<>(ActionResult.SUCCESS, order);

		result.getStatusFlags().add(ActionResult.StatusFlag.OBJECT_PERSISTED);

		return result;
	}

	/**
	 * Returns the confirmation message for the return request action.
	 *
	 * @param actionContext the action context containing the selected order
	 * @return {@code null} because this action does not require a confirmation message
	 */
	@Override
	public String getConfirmationMessage(final ActionContext<OrderModel> actionContext)
	{
		return null;
	}

	/**
	 * Indicates whether user confirmation is required before creating a return request.
	 *
	 * @param actionContext the action context containing the selected order
	 * @return {@code false} because confirmation is not required
	 */
	@Override
	public boolean needsConfirmation(final ActionContext<OrderModel> actionContext)
	{
		return false;
	}
}
