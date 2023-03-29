/// A InterSystems InboundAdapter to receive messages from a SAP system.
Class com.intersystems.dach.ens.sap.PassthroughService Extends Ens.BusinessService
{

Parameter ADAPTER = "com.intersystems.dach.ens.sap.InboundAdapter";

/// Configuration item(s) to which to send file stream messages
Property TargetConfigNames As %String(MAXLEN = 1000);

Parameter SETTINGS = "TargetConfigNames:Basic:selector?multiSelect=1&context={Ens.ContextSearch/ProductionItems?targets=1&productionName=@productionId}";

Method OnProcessInput(pInput As com.intersystems.dach.ens.sap.SAPDataObject, pOutput As %RegisteredObject) As %Status
{
   #dim tSC As %Status = $$$OK 
   #dim pRequest As Ens.Request

   if (pInput.IsJson) {
      Set pRequest = ##class(Ens.StringRequest).%New(pInput.Data)
   } else {
      Set pRequest = ##class(EnsLib.EDI.XML.Document).%New(pInput.Data)
      Set pRequest.DocType = pInput.FunctionName
   }
   
   For iTarget=1:1:$L(..TargetConfigNames, ",") {
      Set tOneTarget=$ZStrip($P(..TargetConfigNames,",",iTarget),"<>W")
      Continue:""=tOneTarget
      $$$sysTRACE("Sending message to '"_tOneTarget_"'")
      set tSC = ..SendRequestAsync(tOneTarget, pRequest)
   }

   Quit tSC
}

/// Return an array of connections for drawing lines on the config diagram
ClassMethod OnGetConnections(Output pArray As %String, pItem As Ens.Config.Item)
{
	Do ##super(.pArray,pItem)
	If pItem.GetModifiedSetting("TargetConfigNames",.tValue) {
		For i=1:1:$L(tValue,",") { Set tOne=$ZStrip($P(tValue,",",i),"<>W")  Continue:""=tOne  Set pArray(tOne)="" }
	}
}

}