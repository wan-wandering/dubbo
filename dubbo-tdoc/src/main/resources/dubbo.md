**001.Comparator的设计**。Prioritized接口继承Comparable接口并以default默认方法的方式实现了，以及提供了外界直接访问的Comparator对象，以及getPriority方法可以被子类重写实现不同子类的自己的优先级定义。详见Prioritized、PrioritizedTest

**002.泛型设计。**Converter<S, T>接口本身支持S->T类型的转化，但是StringConverter子接口的引入限定了仅支持String->T的转化，子类只能传递一个T。

**003.类型转化设计。**Converter SPI接口有多个扩展类，支持String->T的转化。用户给定的S->T是否支持转化，内部用了很多泛型参数、获取父类、父接口等相关API的操作以及结合lambda流式处理和过滤。比如StringToBooleanConverter在accept判断的时候就会获取String.class和Integer.class。

**004.MultiConverter和Converter的设计。**都是accept+convert+getConverter/find几个方法。以及都继承了Prioritized，用以在find的时候优先对优先级高的进行accept判定。StringToIterableConverter的Convert方法用了模板方法设计模式。

**005.Completable异步编程技巧。**ScheduledCompletableFuture.submit提交一个任务，返回CompletableFuture对象，把这个future填充到集合里面，在某处会遍历该集合，判断future.isDone并作对应的处理，比如isDone=false，进行future.cancel。isDone完成的时间点在future.complete(xx)的触发，详见ScheduledCompletableFuture。**实现一个带有返回结果的任务**不一定使用callable，可以直接用CompletableFuture。

**006.自定义线程池的拒绝策略。**详见AbortPolicyWithReport。在原生AbortPolicy的基础上做了一些扩展功能：log、dumpJStack、dispatchThreadPoolExhaustedEvent。以及涉及到了事件监听模型，在线程池Exhausted的时候，构建ThreadPoolExhaustedEvent并派遣给对应的监听器处理。

**007.ThreadPool的设计**。ThreadPool是一个**SPI**接口，扩展类给出了不同线程池的实现，**核心还是利用ThreadPoolExecutor**，只是参数不同，参数大都从URL取得，以及用了**自定义的线程工厂、拒绝策略**等。特别看了下**EagerThreadPoolExecutor和TaskQueue**，前者属于自定义的线程池，**重写了afterExecute和execute方法做一些前置后置操作**，以及在发生拒绝异常的时候重试投递到TaskQueue，且**TaskQueue的offer重写了，使得不一定任务队列满了才会创建超过核心线程数大小的线程。**

**008.更快速的ThreadLocal。**涉及到的类型InternalThread、InternalThreadLocal、InternalThreadMap。快速体现在使用了数组存储，线程副本实现思想和原生ThreadLocal一致。

**009.注解工具类。** 判断某个类上的注解是否是某个注解（isSameType）、获取注解里面的值（getAttribute）、获取类上的所有注解并提供带谓词过滤的（getDeclaredAnnotations）、获取类以及所有父类、父接口的注解、获取元注解、获取所有的元注解包括元注解本身的元注解、类上是否匹配多个注解、根据注解的全限定名获取类的对应注解（testGetAnnotation）、

**010.UnmodifiableXXX、SingletonList。**很多工具类返回的集合等数据都用unmodifiableXXX包装了，防止修改。

**011.AnnotatedElement是Class和Method的父接口**，getDeclaredAnnotations方法用AnnotatedElement来接受的原因是：有时候我们不仅仅想获取类上的注解（传入A.class），还想获取方法上的注解（传入的是Method m）虽然Class和Method都有getDeclaredAnnotations，但是为了通用，就都用AnnotatedElement接受，一定程度的抽取公共部分解耦。

**012.ExecutorService线程池的优雅关闭。** shutdwon+awaitTermination+shutdownNow+线程循环shutdownNow详见ExecutorUtil。

**013.ClassUtil实现了自己的forName。**涉及到线程上下文加载器、loadClass api等。

**014.很多工具类都是私有化自己的构造方法的。**还有一些单例模式，比如CharSequenceComparator。

**015.ConcurrentHashSet的实现借助了ConcurrentHashMap**。内部的value用present填充。

**016.${}占位符的解析和替换。**主要利用了Pattern、Matcher，详见replaceProperty方法。

**017.线程上下文加载器加载resources下的文件。**在当前模块下对resources目录下的的文件进行file.exist()都会返回false，需要利用线程上下文加载器加载。getResourceAsStream、getResources、ClassUtils.getClassLoader().getResources(fileName)

**018.LFU的设计。**存储结构：Map+CacheDeque，队列之间用CacheDeque.nextDeque指针连接，put的元素永远放在第0个队列最后，当超过容量大小开始驱逐（驱逐的个数取决于驱逐因子evictionFactor的值），每次get元素的时候影响到元素的访问次数，get会将元素所在的队列迁移到挨着的下一个队列，访问次数越多，越靠后。

**019.LRU的设计**。LinkedHashMap+锁保证安全。

**020.Dubbo基于Spring提供的NamespaceHandler和BeanDefinitionParser来扩展了自己XML Schemas。**实现spring中自定义xml标签并解析一般需要四个步骤:提供自己的BeanDefinition解析器、命名空间处理器（注册前面自己的BeanDefinition解析器）、配置spring.handlers用以关联命名空间处理器和xsd中的targetNamespace、配置spring.schemas指定dubbo.xsd的路径。

**021.SpringExtensionFactory。**dubbo的其中一种容器工厂，用于获取spring相关的bean，内部有缓存所有add进来的ioc容器，获取bean的时候实际是循环遍历从ioc容器获取。不同容器的beanName是可以重复的。

**022.责任链模式，扩展类的Wrapper类。** 去看下上面Protocol$Adaptive的export方法，最后进入DubboProtocol的export方法，但是注意了！！！getExtension最后返回的是QosProtocolWrapper，原因是因为在getExtension内部处理会调用createExtension(String name, boolean wrap)，且默认wrap是ture即扩展类实例（比如DubboProtocol）需要被包装，对于Protocol来说在loadClass的时候有三个WrapperClass（根据是否含有拷贝构造函数），分别是QosProtocolWrapper、ProtocolFilterWrapper、ProtocolListenerWrapper，按照@Activate(order=xx)的值以及WrapperComparator.COMPARATOR进行排序，然后千层饼一样包装（其实是责任链模式），最后QosProtocolWrapper（ProtocolFilterWrapper（ProtocolListenerWrapper（DubboProtocol））））然后export一层层深入调用，每层加了自己的逻辑

**023.ProxyFactory**。含有getProxy和getInvoker，利用jdk和Javassist生成基于接口的代理类。

**024.hashCode和equals的常见写法。**hashcode是 result = primie*result + att==null?0:attr.hashCode()，equals就不说了。随便找一个参考下吧

**025.从getXXX方法提取XXX提取以及根据驼峰转为split分割的字符串。** 详见calculatePropertyFromGetter和camelToSplitName。

**026.值结果参数。**new一个对象，作为参数传入到一个不带返回值的方法，内部会对这个参数填值。ApplicationConfigTest.testName测程序。

**027.CompositeConfiguration的设计+AbstractConfig的refresh操作。**CompositeConfiguration是一种组合不同场景Configuration的Configuration；AbstractConfig的refresh操作，把CompositeConfiguration的一些参数赋值给AbstractConfig的一些属性赋值，且考虑到不同场景Configuration的优先级，同名key优先取谁的值。

**028.一些恢复现场的操作。**test程序System.setProperty(x,y)在最后一定clearProperty、filed.isAccessable() = false的时候设置为true最后在还原为false。

**029.Properties和文件的交互。**properties.load(this.getClass().getResourceAsStream("/dubbo.properties"));

**030.两个AbstractConfig的子类对象是否equals的逻辑。**name相同、两个对象同名同参的getXx方法的返回值相同。还有个小技巧就是如果@parameter注解带有exclued为true，那么不参与比较。

**031.延迟加载。**compositeConfiguration内部匹配到PropertiesConfiguration有属性xx参数的话，在PropertiesConfiguration.getInternalProperty内部有延迟加载模式，因为加载文件内容涉及到io操作，相对耗时。

**032.isValid方法，动态指定是否可用。**checkRegistry方法：registries里面有很多对象，如果暂时不想某些类型的注册中心对象生效，可以isValid置为false

**033.ConfigManager的read、write方法。**ConfigManager内部关于configsCache的读写业务逻辑操作都封装了runnable任务/callable，并传给write或者后面的read，并且write、read方法内部使用读写锁保护了configCache。

**034.DubboShutdownHook。**在DubboBootstrap的构造方法内部向jvm注册了一个DubboShutdownHook，其run方法主要是执行所有注册的回调以及资源清理动作（还涉及到一些事件派发）。回调的注册在DubboShuthookCallbacks类，填充了很多DubboShuthookCallback(且有优先级)，并根据spi能加载配置的子类对象。注：DubboShutdownHook、DubboShuthookCallbacks都是饿汉单例的。DubboBootStrap是双重检查的单例模式。

**035.类结构设计，接口->Abstract类->实现类**。Abstract里面可以放置一些公共逻辑，实现模板方法模式。比如ZookeeperClient->AbstractZookeeperClient->CuratorZookeeperClient，ZookeeperTransporter->AbstractZookeeperTransporter->CuratorZookeeperTransporter等。

**036.观察者模式。**详见AbstractZookeeperClient的stateChanged方法。被观察者的状态变化，调用所有观察者的方法

036.Curator连接zk的客户端。

**037.连接复用，缓存设计。**AbstractZookeeperTransport的connect方法内部两次查询缓存，都查不到才会创建zkClinet，利用缓存实现连接复用，不会多次创建相同的connection。

**038.抽象工厂模式。**ChannelBufferFactory接口、ChannelBuffer接口以及相关的实现类利用了标准的抽象工厂模式。

**039.动态缓冲区，自动扩容。**详见ensureWritableBytes以及重写的方法（2倍递增直到超过期望的最小目标容量）

**040.过滤器、责任链模式。**在调用Protocol实例的export方法的时候，会经过FilterProtocol的export，内部会取出很多filter进行拦截。我们也可以根据自己进行扩展。

041.AbstractRegistry注册、订阅、同步异步保存文件、文件锁

**042.根据url搞线程池。**AbstractDynamicConfiguration，构造函数会根据url参数来创建线程池（前缀、核心线程从url取），以及getConfig、removeConfig等操作当做一个任务交给线程池执行，以及根据timeout值来决定是带超时的阻塞还是不带...

**043.循环new ServerSocket(port)获取有效端口、AbstractExporter抽象类定义构造方法的意义的就是子类公用父类逻辑。**

**044.DefaultTPSLimiter限流**。限制一个service的调用在interval内至多调用rate+1次、内部用到了longAddr作为次数，以及当前时间>interval+lastRestTime会重置lastResetTime和token。

**045.ActiveLimitFilter限定一个方法的调用次数以及涉及到超时**。如果有超过配置的正在尝试调用远程方法，则调用其余的方法将等待配置的超时(默认为0秒)，然后调用被dubbo终止。里面的一些设计点：双重检查判定次数是否达到active的值，sync+while+wait的惯用法，wait醒来后判断是否超时，超时抛异常。以及一些RpcStatus 的设计（两个维度，service和method），其有一个active属性，在beginCount方法中利用cas+for进行++，endCount反操作，以及在onResponse触发后调用rpcStatus.notifyAll();唤醒在sync等待的线程，RpcStatus还有一些统计指标（成功失败次数、总时长、最大时长）。其测试程序（testInvokeNotTimeOut方法）有个设计点，两个latch分别控制多线程的起点和重点。

**046.ExecuteLimitFilter。**类似于上面的，不过不考虑超时。

**047.ProtocolListenerWrapper**。本身实现了Protocol，这个类是做一个拦截/代理逻辑，在发起其含有的目标对象protocol.export和refer方法后，会在export或refer的返回值之后用ListenerExporterWrapper或者ListenerInvokerWrapper包装起来，并通过spi加载对应的监听器，相当于就是说export之后，那些监听器关心的话，会调用相应的的处理函数。且注意ListenerExporterWrapper或者ListenerInvokerWrapper会返回给调用方，要做到对调用方只关心返回类型是Exporter和Invoker，所以这两个类本身是实现Exporter和Invoker的。

**048.RpcException。**本身继承runtimeException，可以传入code、message、cause，code代表不同场景下的异常，也提供了isXX是否是某种异常，注意在isLimitExceed方法里面的这个api记下：getCause() instanceof LimitExceededException;

**049.RpcContext。**一个临时状态容器。每次发送或接收请求时，RpcContext中的状态都会发生变化。内置核心InternalThreadLocal，LOCAL、SERVER_LOCAL这种，以及一些操作，例如：getContext、removeContext、getServerContext、removeServerContextsetUrl、isConsumerSide、isProviderSide、setLocalAddress、setRemoteAddress、setObjectAttachments（Attachments等相关）、values map容器、AsyncContext、asyncCall。

**050.AppResponse**。result的实现类，主要可以存放值、异常、attachments map、还有一个recreate方法，这个通过循环拿到异常的顶级父类Throwable，然后反射获取stackTrace字段，看是不是null是的话，设置exception.setStackTrace(new StackTraceElement[0]);还接触到UnsupportedOperationException异常。

**051.AsyncRpcResult。**内部主要有一些CompletableFuture的api值得学习、isDone、get(timeout, unit)、complete(xx)、whenComplete(BiConsumer)、thenApply(Function)、completeExceptionally等。

**052.ListenableFilter**。杂类，没啥用。学到一个ConcurrentMap父接口，子类ConcurrentHashMap，比如赋值ConcurrentMap<Invocation, Listener> listeners = new ConcurrentHashMap<>();

**053.InvokeMode。**枚举类，三种调用方式SYNC, ASYNC, FUTURE;

**054.FutureContext。**和RpcContext类似，核心都是利用InternalThreadLocal。主要是这么调用FutureContext.getContext().setFuture(CompletableFuture.completedFuture("future from thread1"));且主要被RpcContext使用。

**055.Constants。**接口的属性、方法不需要public、static前缀，因为自动会加上

**056.AttachmentsAdapter。**其有一个内部类ObjectToStringMap继承了HashMap，其ObjectToStringMap构造方法主要把value转化为String类型。以及ObjectToStringMap是public static修饰的，外部如果要new这个内部类实例的话，这样：new AttachmentsAdapter.ObjectToStringMap(attachments);

**057.AsyncContextImpl。**AsyncContextImpl->start->write/getInternalFuture->stop大概是这个顺序，不知为何里面用started和stopped两个原子类来标记启动、开始。

**058.Rpcutils。** 主要为RpcInvocation服务。

（1）attachInvocationIdIfAsync方法是幂等的，用以给(rpc)Invocation添加id，id是用原子类自增实现唯一性，从0计数，内部根据参数url、invocation是否含有async标记。

（2）RpcUtils.getReturnType(inv)，明白了inv和invoker的关系，inv的含有invoker引用、方法名、参数等信息，这些信息表明这个inv是想要调用invoker的url表示的接口的对应方法，通过invoker.invoke(inv)触发。

（3）RpcUtils.getReturnTypes(inv)获取方法返回类型以及带泛型的返回类型，学到一些反射相关的api泛型method.getReturnType();、method.getGenericReturnType();，isAssignableFrom、instanceof ParameterizedType、((ParameterizedType) genericReturnType).getActualTypeArguments()[0]、(Class<?>) ((ParameterizedType) actualArgType).getRawType()。

（4）测试方法：@ParameterizedTest+@CsvSource({x,y,z})，xyz循环多次作为入参

（5）RPCInvocation指定的方法名指定为$invoke，具体的方法名称、参数类型数组、参数值数组 在parameterTypes、arguments属性给出。比如 parameterTypes = new Class<?>[]{String.class, String[].class, Object[].class},arguments=new Object[]{"method", new String[]{}, new Object[]{"hello", "dubbo", 520}}

**059.MockProtocol。**AbstractProtocol的子类，主要是一个mock程序，其实现了protocolBindingRefer模板方法，内部return new MockInvoker<>(url, type);

**060.MockInvoker。** 内有方法parseMockValue、new MockInvoker(url, String.class);、mockInvoker.invoke(invocation)常见方法，主要是做mock测试的，最核心的就是Result invoke(Invocation invocation)方法，会从url获取mock的值（mock值三种情况: return xx 、 throw xx 、接口impl全限定名）、以及有normalizeMock标准化mock值。

**061.AsyncToSyncInvoker。**invoker.invoke(invocation);之后如果发现是同步的，那么会get()阻塞直接结果返回方法才返回。

**062.DubboInvoker。**AbstractInvoker的具体子类，主要在DubboProtocol得到构造。最关键的就是doInvoke模板方法的实现，给inv填充一些属性后，根据url+inv判断是否是**onway**请求，向ExchangeClient发送send或者request方法调用，最后结果都是以AsyncRpcResult返回；选择ExchangeClient的时候是从构造函数传进来的clients数组**轮询**选择的。其isAvailable方法判断拥有的clients数组，有一个可用（连接状态+没有可读属性）就代表可用。其destroy方法利用**双重检查（lock锁）防止重复关闭**。

**064.DubboProtocol。**ProxyFactory、Protocol、Invoker关系:protocol.export(proxy.getInvoker

**065.DubboProtocolServer。** ProtocolServer子类，存放RemotingServer和address的。

**066.ServerStatusChecker。**检查DubboProtocol实例持有的RemotingServer，看是否isBound，然后设置level和sb填充到Status。

**067.Status。** 给StatusChecker用的，三个核心属性level+message+desc。

**068.StatusChecker。**spi接口，唯一方法Status check();。

**069.SimpleDataStore。**DataStore spi接口的唯一实现。主要在内存级别维护了一个双key的map，<component name or id, <data-name, data-value>>

**070.ThreadPoolStatusChecker。**主要是检查线程池的一些状态，几个api学习下：getMaximumPoolSize、getCorePoolSize、getLargestPoolSize、getActiveCount、getTaskCount。maximumPoolSize:是一个静态变量,在变量初始化的时候,有构造函数指定.largestPoolSize: 是一个动态变量,是记录Poll曾经达到的最高值,也就是 largestPoolSize<= maximumPoolSize.

**071.Codec2。**SPI接口，codec 编码-解码器“Coder-Decoder”的缩写，接口两个方法encode和decode。

**072.CodecAdapter。**该类实现Codec2接口，实现了标准的适配器模式，实现目标接口，含有被适配器对象，主要是兼容旧版本。旧版本的Codec接口（没有2）的encode和decode的参数没有buffer，而是io输入输出流，也说明buffer既可以作为输入也可以作为输出，这点比io流更好。

**073.UnsafeByteArrayOutputStream。** extends OutputStream，核心属性：字节数组和填充的字节数，支持自动扩容。注意几个api：System.arraycopy(b, off, mBuffer, mCount, len) 、 new String(mBuffer, 0, mCount, charset);指定字符编码、ByteBuffer.wrap(mBuffer, 0, mCount);将字节数组转化为ByteBuffer。

**074.AbstractCodec。**Codec的抽象实现类，checkPayload检查size是否超过payLoad，isClientSide判断是否是客户端（从channel的属性获取、获取不到的话利用url和channel.getRemoteAddress比较，这里得知channel有localAddress和remoteAddress）

**075.SerializableClassRegistry、SerializationOptimize。**序列化优化器和序列化类注册容器。没啥调用点。