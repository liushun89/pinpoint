package com.navercorp.pinpoint.plugin.dubbo;

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;

import java.security.ProtectionDomain;

/**
 * @author Jinkai.Ma
 */
public class DubboPlugin implements ProfilerPlugin, TransformTemplateAware {

    private TransformTemplate transformTemplate;

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        this.addApplicationTypeDetector(context);
        this.addTransformers();
    }

    private void addTransformers() {
        transformTemplate.transform("com.alibaba.dubbo.rpc.cluster.support.AbstractClusterInvoker", new TransformCallback() {
            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

                target.getDeclaredMethod("invoke", "com.alibaba.dubbo.rpc.Invocation").addInterceptor("com.navercorp.pinpoint.plugin.dubbo.interceptor.DubboConsumerInterceptor");

                return target.toBytecode();
            }
        });
        transformTemplate.transform("com.alibaba.dubbo.rpc.proxy.AbstractProxyInvoker", new TransformCallback() {
            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

                target.getDeclaredMethod("invoke", "com.alibaba.dubbo.rpc.Invocation").addInterceptor("com.navercorp.pinpoint.plugin.dubbo.interceptor.DubboProviderInterceptor");

                return target.toBytecode();
            }
        });
    }

    /**
     * Pinpoint profiler agent uses this detector to find out the service type of current application.
     */
    private void addApplicationTypeDetector(ProfilerPluginSetupContext context) {
        context.addApplicationTypeDetector(new DubboProviderDetector());
    }

    @Override
    public void setTransformTemplate(TransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }
}
