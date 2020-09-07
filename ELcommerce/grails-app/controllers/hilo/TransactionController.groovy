package hilo

import groovy.text.SimpleTemplateEngine

import org.springframework.dao.DataIntegrityViolationException
import io.hilo.BaseController
import io.hilo.common.OrderStatus
import io.hilo.Transaction
import grails.converters.*

import com.easypost.EasyPost
import com.easypost.model.Rate
import com.easypost.model.Address
import com.easypost.model.Parcel
import com.easypost.model.Shipment
import com.easypost.exception.EasyPostException
import grails.util.Environment

import io.hilo.api.mail.EasyPostShipmentApi

import com.stripe.Stripe
import com.stripe.model.Charge
import com.stripe.model.Refund

import grails.plugin.springsecurity.annotation.Secured

import io.hilo.api.payment.StripePaymentsProcessor
import io.hilo.api.payment.BraintreePaymentsProcessor
import io.hilo.api.payment.PaymentCharge

import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder


@Mixin(BaseController)
class TransactionController {

    static allowedMethods = [ send_confirmation: "POST", update_status: "POST", refund: 'POST', delete: "POST" ]


	def emailService
	def currencyService
	def applicationService
	
	MessageSource messageSource

	@Secured(['ROLE_ADMIN', 'ROLE_CUSTOMER'])
	def details(Long id){
		authenticatedPermittedOrderDetails { customerAccount, transactionInstance ->
	    	[transactionInstance: transactionInstance]
		}
	}
	
	@Secured(['ROLE_ADMIN'])
    def list(Integer max) {
		authenticatedAdmin { adminAccount ->
        	params.max = Math.min(max ?: 10, 100)
        	params.sort = "id"
        	params.order = "desc"
        	[transactionInstanceList: Transaction.list(params), transactionInstanceTotal: Transaction.count()]
		}
    }
	
	@Secured(['ROLE_ADMIN'])
    def show(Long id) {
		authenticatedAdminTransaction { adminAccount, transactionInstance ->
			[transactionInstance: transactionInstance]
		}
    }


	@Secured(['ROLE_ADMIN'])
	def update_status(Long id){
		authenticatedAdminTransaction { adminAccount, transactionInstance ->
			if(!params.status){
				flash.message = messageSource.getMessage("transaction.set.status", null, LocaleContextHolder.locale)
				redirect(action : 'show', id : transactionInstance.id )
				return
			}
			
			transactionInstance.status = params.status
			transactionInstance.save(flush:true)
			flash.message = messageSource.getMessage("successfully.update.order", null, LocaleContextHolder.locale)
			redirect(action : 'show', id : transactionInstance.id )
		}
	}


	@Secured(['ROLE_ADMIN'])
    def delete(Long id) {
		authenticatedAdminTransaction { adminAccount, transactionInstance ->
        	try {
				def shoppingCart = transactionInstance.shoppingCart
				
        	    transactionInstance.delete(flush: true)
				shoppingCart.delete(flush: true)
				
        	    flash.message = messageSource.getMessage("successfully.deleted", null, LocaleContextHolder.locale)
        	    redirect(action: "list")
        	}catch (DataIntegrityViolationException e) {
        	    flash.message = messageSource.getMessage("something.went.wrong.message", null, LocaleContextHolder.locale)
        	    redirect(action: "show", id: id)
        	}
		
		}
	}
	
	
	@Secured(['ROLE_ADMIN'])
	def confirm_purchase_shipping_label(Long id){
		authenticatedAdminTransaction { adminAccount, transactionInstance ->
			[ transactionInstance : transactionInstance ]
		}
	}
	
	
	
	@Secured(['ROLE_ADMIN'])
	def purchase_shipping_label(Long id){
		authenticatedAdminTransaction { adminAccount, transactionInstance ->
		
			if(!transactionInstance.shoppingCart.shipmentId){
				flash.message = messageSource.getMessage("shipment.id.specified", null, LocaleContextHolder.locale)
				redirect( action : 'show', id : id )
				return
			}
			
			if(!transactionInstance.shoppingCart.shipmentRateId){
				flash.message = messageSource.getMessage("shipment.rate.specified", null, LocaleContextHolder.locale)
				redirect( action : 'show', id : id )
				return
			}
			
			
			try{
			
				def shippingApi = new EasyPostShipmentApi(applicationService, currencyService)
				
				
				def shipmentId = transactionInstance.shoppingCart.shipmentId
				def shipmentRateId = transactionInstance.shoppingCart.shipmentRateId
				def postage = shippingApi.buyShippingLabel(shipmentId, shipmentRateId)
				
				if(!postage){
					flash.message = messageSource.getMessage("something.went.wrong.shipping.label", null, LocaleContextHolder.locale)
					redirect(action: 'show', id : id)
					return
				}
				
				transactionInstance.postageId = postage.id
				transactionInstance.postageUrl = postage.labelUrl
				transactionInstance.save(flush:true)
				
				[ transactionInstance : transactionInstance ]
    
	    	}catch (Exception e) {
				println e
				//TODO: uncomment 
				//e.printStackTrace()
        	    flash.message = messageSource.getMessage("something.went.wrong.message", null, LocaleContextHolder.locale)
        	    redirect(action: "show", id: id)
				return
        	}
			
		}
	}
	
	
	@Secured(['ROLE_ADMIN'])
	def print_shipping_label(Long id){
		authenticatedAdminTransaction { adminAccount, transactionInstance ->
			[transactionInstance : transactionInstance]
		}
	}
	
	
	
	@Secured(['ROLE_ADMIN'])
	def confirm_refund(Long id){
		authenticatedAdminTransaction { adminAccount, transactionInstance ->
			[ transactionInstance : transactionInstance ]
		}
	}
	
	
	
	@Secured(['ROLE_ADMIN'])
	def refund(Long id){
		authenticatedAdminTransaction { adminAccount, transactionInstance ->
		
			try{

				def paymentsProcessor = new StripePaymentsProcessor(applicationService, currencyService)
				
				if(transactionInstance.gateway == PaymentCharge.BRAINTREE){
					println "tr 191 : braintree -> " 
					paymentsProcessor = new BraintreePaymentsProcessor(applicationService, currencyService)
				}
				
				def refundedCharge = paymentsProcessor.refund(transactionInstance.chargeId)
				println "tr 191 -> " + refundedCharge.id
						
				transactionInstance.refundChargeId = refundedCharge.id
				transactionInstance.status = OrderStatus.REFUNDED.description()
				transactionInstance.save(flush:true)
			
				flash.message = messageSource.getMessage("successfully.refunded.order", [ id ] as Object[], "Default", LocaleContextHolder.locale)
				redirect(action : 'show', id : id)
			
			}catch (Exception e){
				//println e
				e.printStackTrace()
				if(e.message.indexOf("has already been refunded") >= 0){
					if(transactionInstance.status != OrderStatus.REFUNDED.description()){
						transactionInstance.status = OrderStatus.REFUNDED.description()
						transactionInstance.save(flush:true)
					}
					flash.message = messageSource.getMessage("already.refunded", [ id ] as Object[], "Default", LocaleContextHolder.locale)
				}else{
					flash.message = messageSource.getMessage("unable.process.refund", [transactionInstance.gateway] as Object[], "Default", LocaleContextHolder.locale)
				}
				redirect(action : 'show', id : id)
				return
			}
		}	
	}
	
	
	@Secured(['ROLE_ADMIN'])
	def send_confirmation(Long id){
		def transactionInstance = Transaction.get(id)
		
		if(!transactionInstance){
			flash.message = messageSource.getMessage("transaction.not.found", null, LocaleContextHolder.locale)
			redirect(action:"")
			return
		}

		try { 
			sendNewOrderEmail(transactionInstance)
		}catch(Exception e){
			e.printStackTrace()
			flash.message = messageSource.getMessage("something.went.wrong", null, LocaleContextHolder.locale)
			redirect(action: "show", id: id)
			return
		}	

		flash.message = messageSource.getMessage("successfully.sent.email.confirmation", null, LocaleContextHolder.locale)
		redirect(action : 'show', id : id)
		return
	}
	
	def sendNewOrderEmail(transaction){
		def fromAddress = applicationService.getSupportEmailAddress()
		def customerToAddress = transaction?.account?.email
		def customerSubject = "${applicationService.getStoreName()} : " + messageSource.getMessage("your.order.placed", null, LocaleContextHolder.locale)
		
		File templateFile = grailsAttributes.getApplicationContext().getResource(  "/templates/email/order_confirmation.html").getFile();
    	
		
		def binding = [ "companyName"  : applicationService.getStoreName(),
			 			"supportEmail" : applicationService.getSupportEmailAddress(),
						"subtotal"     : applicationService.formatPrice(transaction.subtotal),
						"taxes"        : applicationService.formatPrice(transaction.taxes),
						"shipping"     : applicationService.formatPrice(transaction.shipping),
						"total"        : applicationService.formatPrice(transaction.total),
						"transaction"  : transaction,
						"orderNumber"  : transaction.id ]
						
		def engine = new SimpleTemplateEngine()
		def template = engine.createTemplate(templateFile).make(binding)
		def bodyString = template.toString()	
    	
    	
					
		def orderDetails = ""
		transaction.shoppingCart.shoppingCartItems.each {
			def optionsTotal = 0
			def optionsString = "<div style=\"font-size:11px; color:#777\">"
			
			if(it.shoppingCartItemOptions?.size() > 0){
				optionsString += "<strong>options : </strong>"
				it.shoppingCartItemOptions.each(){ option ->
					optionsTotal += option.variant.price
					optionsString += "${option?.variant?.name}"
					optionsString += "(${currencyService.format(applicationService.formatPrice(option.variant.price))})<br/>"
				}	
			}
			optionsString += "</div>"
		
			def productTotal = it.product.price + optionsTotal
    	
			def extendedPrice = productTotal * it.quantity
			
			orderDetails += "<tr>"
			orderDetails += "<td style=\"text-align:center; padding:3px; border-bottom:dotted 1px #ddd\">${it.product.id}</td>"
			orderDetails += "<td style=\"padding:3px; border-bottom:dotted 1px #ddd\">${it.product.name}${optionsString}</td>"
			orderDetails += "<td style=\"text-align:center; padding:3px; border-bottom:dotted 1px #ddd\">${currencyService.format(applicationService.formatPrice(productTotal))}</td>"
			orderDetails += "<td style=\"text-align:center; padding:3px; border-bottom:dotted 1px #ddd\">${it.quantity}</td>"
			orderDetails += "<td style=\"text-align:center; padding:3px; border-bottom:dotted 1px #ddd\">${currencyService.format(applicationService.formatPrice(extendedPrice))}</td>"
			orderDetails += "</tr>"
		}
		
		bodyString = bodyString.replace("[[ORDER_DETAILS]]", orderDetails)
		
		def adminEmail = applicationService.getAdminEmailAddress()
		def allEmails = customerToAddress
		if(adminEmail){
		 	allEmails += ",${adminEmail}"
		}
		
		emailService.send(allEmails, fromAddress, customerSubject, bodyString)
		
	}
	
}
