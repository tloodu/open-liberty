/**
# COPYRIGHT LICENSE:
# This information contains sample code provided in source code form. You may
# copy, modify, and distribute these sample programs in any form without
# payment to IBM for the purposes of developing, using, marketing or
# distributing application programs conforming to the application programming
# interface for the operating platform for which the sample code is written.
# Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE
# ON AN "AS IS" BASIS AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED,
# INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED WARRANTIES OR CONDITIONS OF
# MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE,
# TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES
# ARISING OUT OF THE USE OR OPERATION OF THE SAMPLE SOURCE CODE. IBM HAS NO
# OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS OR
# MODIFICATIONS TO THE SAMPLE SOURCE CODE.
 **/

package com.ibm.jaxws.attachment.mtom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.jws.HandlerChain;
import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.SOAPBinding;

@javax.jws.WebService(endpointInterface = "com.ibm.jaxws.attachment.mtom.MtomSample",
                      targetNamespace = "http://com/ibm/jaxws/attachment/mtom/",
                      serviceName = "MtomSampleService",
                      portName = "MtomSamplePort",
                      wsdlLocation = "wsdl/ImageDepot.wsdl")
@BindingType(value = SOAPBinding.SOAP11HTTP_MTOM_BINDING)
@HandlerChain(file = "soaphandlers.xml")
public class MtomSamplePortImpl {

    @Resource
    WebServiceContext wsc;

    public ImageDepot sendImage(ImageDepot data) {
        try {
            // read all data
            List<DataHandler> dhList = data.getImageData();
            for (DataHandler dh : dhList) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                dh.writeTo(bos);
                bos = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
}
