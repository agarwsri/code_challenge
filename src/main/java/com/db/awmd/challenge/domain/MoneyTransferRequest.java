package com.db.awmd.challenge.domain;

import java.math.BigDecimal;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MoneyTransferRequest {

	@NotBlank
	private String fromAccountId;
	
	@NotBlank
	private String toAccountId;
	
	@NotNull
	@Min(value = 0,message = "amountToTransfer should be a positive value")
	private BigDecimal amountToTransfer;
	
	 @JsonCreator
	  public MoneyTransferRequest(@JsonProperty("fromAccountId") String fromAccountId,
	    @JsonProperty("toAccountId") String toAccountId, @JsonProperty("amountToTransfer") BigDecimal amountToTransfer) {
	    this.fromAccountId = fromAccountId;
	    this.toAccountId = toAccountId;
	    this.amountToTransfer = amountToTransfer;
	  }
}
