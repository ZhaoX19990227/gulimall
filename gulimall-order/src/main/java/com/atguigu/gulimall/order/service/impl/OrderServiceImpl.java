package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.to.OrderTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.order.constant.PayConstant;
import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.PaymentInfoEntity;
import com.atguigu.gulimall.order.enume.OrderStatusEnum;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WmsFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.service.PaymentInfoService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.to.SpuInfoVo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.atguigu.common.constant.CartConstant.CART_PREFIX;
import static com.atguigu.gulimall.order.constant.OrderConstant.USER_ORDER_TOKEN_PREFIX;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    MemberFeignService memberFeignService;
    @Autowired
    CartFeignService cartFeignService;
    @Autowired
    ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    WmsFeignService wmsFeignService;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private PaymentInfoService paymentInfoService;

    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        //??????OrderConfirmVo
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        //?????????????????????????????????
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();

        //TODO :?????????????????????????????????(??????Feign?????????????????????????????????)
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        //???????????????????????????
        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {

            //????????????????????????????????????????????????
            RequestContextHolder.setRequestAttributes(requestAttributes);

            //1??????????????????????????????????????????
            List<MemberAddressVo> address = memberFeignService.getAddress(memberResponseVo.getId());
            confirmVo.setMemberAddressVos(address);
        }, threadPoolExecutor);

        //???????????????????????????
        CompletableFuture<Void> cartInfoFuture = CompletableFuture.runAsync(() -> {

            //????????????????????????????????????????????????
            RequestContextHolder.setRequestAttributes(requestAttributes);

            //2????????????????????????????????????????????????
            List<OrderItemVo> currentCartItems = cartFeignService.getCurrentCartItems();
            confirmVo.setItems(currentCartItems);
            //feign???????????????????????????????????????????????????????????????
        }, threadPoolExecutor).thenRunAsync(() -> {
            List<OrderItemVo> items = confirmVo.getItems();
            //?????????????????????id
            List<Long> skuIds = items.stream()
                    .map((itemVo -> itemVo.getSkuId()))
                    .collect(Collectors.toList());

            //??????????????????????????????
            R skuHasStock = wmsFeignService.getSkuHasStock(skuIds);
            List<SkuStockVo> skuStockVos = skuHasStock.getData("data", new TypeReference<List<SkuStockVo>>() {
            });

            if (skuStockVos != null && skuStockVos.size() > 0) {
                //???skuStockVos???????????????map
                Map<Long, Boolean> skuHasStockMap = skuStockVos.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(skuHasStockMap);
            }
        }, threadPoolExecutor);

        //3?????????????????????
        Integer integration = memberResponseVo.getIntegration();
        confirmVo.setIntegration(integration);

        //4???????????????????????????

        //TODO 5???????????????(????????????????????????)
        //?????????????????????token????????????????????????????????????redis???
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId(), token, 30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);

        CompletableFuture.allOf(addressFuture, cartInfoFuture).get();

        return confirmVo;
    }


    /**
     * ????????????
     *
     * @param vo
     * @return
     */
    // @Transactional(isolation = Isolation.READ_COMMITTED) ???????????????????????????
    // @Transactional(propagation = Propagation.REQUIRED)   ???????????????????????????
    @Transactional(rollbackFor = Exception.class)
    //@GlobalTransactional(rollbackFor = Exception.class)
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {

        confirmVoThreadLocal.set(vo);

        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        //????????????????????????????????????????????????????????????...

        //?????????????????????????????????
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        responseVo.setCode(0);

        //1??????????????????????????????????????????????????????????????????????????????
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();

        //??????lua???????????????????????????????????????
        Long result = (Long) redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId()),
                orderToken);

        if (result == 0L) {
            //??????????????????
            responseVo.setCode(1);
            return responseVo;
        } else {
            //??????????????????
            //1????????????????????????????????????
            OrderCreateTo order = createOrder();

            //2???????????????
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();

            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                //????????????
                //3???????????????
                saveOrder(order);
                //4???????????????,????????????????????????????????????
                //?????????????????????????????????(skuId,skuNum,skuName)
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                //???????????????????????????????????????
                List<OrderItemVo> orderItemVos = order.getOrderItems().stream().map((item) -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(orderItemVos);

                //?????????????????????????????????
                //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????(???????????????seata)
                //???????????????????????????????????????seata???????????????????????????????????????????????????,??????????????????????????????
                R r = wmsFeignService.orderLockStock(lockVo);
                if (r.getCode() == 0) {
                    //????????????
                    responseVo.setOrder(order.getOrder());
                    // int i = 10 / 0;  //???????????????????????????
                    // ????????????????????????????????????MQ
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
                    //???????????????????????????
                    redisTemplate.delete(CART_PREFIX + memberResponseVo.getId());
                    return responseVo;
                } else {
                    //????????????
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                    // responseVo.setCode(3);
                    // return responseVo;
                }

            } else {
                responseVo.setCode(2);
                return responseVo;
            }
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param orderSn
     * @return
     */
    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {

        OrderEntity orderEntity = this.baseMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));

        return orderEntity;
    }

    /**
     * ????????????
     *
     * @param orderEntity
     */
    @Override
    public void closeOrder(OrderEntity orderEntity) {

        //?????????????????????????????????????????????????????????????????????????????????
        OrderEntity orderInfo = this.getOne(new QueryWrapper<OrderEntity>().
                eq("order_sn", orderEntity.getOrderSn()));

        if (orderInfo.getStatus().equals(OrderStatusEnum.CREATE_NEW.getCode())) {
            //???????????????????????????
            OrderEntity orderUpdate = new OrderEntity();
            orderUpdate.setId(orderInfo.getId());
            orderUpdate.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(orderUpdate);

            // ???????????????MQ
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderInfo, orderTo);

            try {
                //TODO ?????????????????????????????????????????????????????????????????????(???????????????????????????????????????)?????????????????????????????????
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            } catch (Exception e) {
                //TODO ???????????????????????????????????????????????????
            }
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param orderSn
     * @return
     */
    @Override
    public PayVo getOrderPay(String orderSn) {

        PayVo payVo = new PayVo();
        OrderEntity orderInfo = this.getOrderByOrderSn(orderSn);

        //????????????????????????????????????
        BigDecimal payAmount = orderInfo.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(payAmount.toString());
        payVo.setOut_trade_no(orderInfo.getOrderSn());

        //????????????????????????
        List<OrderItemEntity> orderItemInfo = orderItemService.list(
                new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity orderItemEntity = orderItemInfo.get(0);
        payVo.setBody(orderItemEntity.getSkuAttrsVals());

        payVo.setSubject(orderItemEntity.getSkuName());

        return payVo;
    }

    /**
     * ????????????????????????????????????
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {

        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();

        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
                        .eq("member_id", memberResponseVo.getId()).orderByDesc("create_time")
        );

        //????????????????????????
        List<OrderEntity> orderEntityList = page.getRecords().stream().map(order -> {
            //??????????????????????????????????????????
            List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>()
                    .eq("order_sn", order.getOrderSn()));
            order.setOrderItemEntityList(orderItemEntities);
            return order;
        }).collect(Collectors.toList());

        page.setRecords(orderEntityList);

        return new PageUtils(page);
    }

    /**
     * ??????????????????????????????
     *
     * @param asyncVo
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String handlePayResult(PayAsyncVo asyncVo) {

        //????????????????????????
        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setOrderSn(asyncVo.getOut_trade_no());
        paymentInfo.setAlipayTradeNo(asyncVo.getTrade_no());
        paymentInfo.setTotalAmount(new BigDecimal(asyncVo.getBuyer_pay_amount()));
        paymentInfo.setSubject(asyncVo.getBody());
        paymentInfo.setPaymentStatus(asyncVo.getTrade_status());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setCallbackTime(asyncVo.getNotify_time());
        //?????????????????????
        this.paymentInfoService.save(paymentInfo);

        //??????????????????
        //??????????????????
        String tradeStatus = asyncVo.getTrade_status();

        if (tradeStatus.equals("TRADE_SUCCESS") || tradeStatus.equals("TRADE_FINISHED")) {
            //??????????????????
            String orderSn = asyncVo.getOut_trade_no(); //???????????????
            this.updateOrderStatus(orderSn, OrderStatusEnum.PAYED.getCode(), PayConstant.ALIPAY);
        }

        return "success";
    }

    /**
     * ??????????????????
     *
     * @param orderSn
     * @param code
     */
    private void updateOrderStatus(String orderSn, Integer code, Integer payType) {

        this.baseMapper.updateOrderStatus(orderSn, code, payType);
    }

    private OrderCreateTo createOrder() {

        OrderCreateTo createTo = new OrderCreateTo();

        //1??????????????????
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = builderOrder(orderSn);

        //2??????????????????????????????
        List<OrderItemEntity> orderItemEntities = builderOrderItems(orderSn);

        //3?????????(??????????????????????????????)
        computePrice(orderEntity, orderItemEntities);

        createTo.setOrder(orderEntity);
        createTo.setOrderItems(orderItemEntities);

        return createTo;
    }

    /**
     * ????????????????????????
     *
     * @param orderCreateTo
     */
    private void saveOrder(OrderCreateTo orderCreateTo) {

        //??????????????????
        OrderEntity order = orderCreateTo.getOrder();
        order.setModifyTime(new Date());
        order.setCreateTime(new Date());
        //????????????
        this.baseMapper.insert(order);

        //?????????????????????
        List<OrderItemEntity> orderItems = orderCreateTo.getOrderItems();
        //???????????????????????????
        orderItemService.saveBatch(orderItems);
    }

    /**
     * ??????????????????
     *
     * @param orderSn
     * @return
     */
    private OrderEntity builderOrder(String orderSn) {

        //??????????????????????????????
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setMemberId(memberResponseVo.getId());
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberUsername(memberResponseVo.getUsername());

        OrderSubmitVo orderSubmitVo = confirmVoThreadLocal.get();

        //???????????????????????????????????????
        R fareAddressVo = wmsFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareResp = fareAddressVo.getData("data", new TypeReference<FareVo>() {
        });

        //?????????????????????
        BigDecimal fare = fareResp.getFare();
        orderEntity.setFreightAmount(fare);

        //???????????????????????????
        MemberAddressVo address = fareResp.getAddress();
        //?????????????????????
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());

        //?????????????????????????????????
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);
        orderEntity.setConfirmStatus(0);
        return orderEntity;
    }


    /**
     * ???????????????????????????
     *
     * @return
     */
    public List<OrderItemEntity> builderOrderItems(String orderSn) {

        List<OrderItemEntity> orderItemEntityList = new ArrayList<>();

        //????????????????????????????????????
        List<OrderItemVo> currentCartItems = cartFeignService.getCurrentCartItems();
        if (currentCartItems != null && currentCartItems.size() > 0) {
            orderItemEntityList = currentCartItems.stream().map((items) -> {
                //?????????????????????
                OrderItemEntity orderItemEntity = builderOrderItem(items);
                orderItemEntity.setOrderSn(orderSn);

                return orderItemEntity;
            }).collect(Collectors.toList());
        }

        return orderItemEntityList;
    }

    /**
     * ?????????????????????????????????
     *
     * @param items
     * @return
     */
    private OrderItemEntity builderOrderItem(OrderItemVo items) {

        OrderItemEntity orderItemEntity = new OrderItemEntity();

        //1????????????skuId
        Long skuId = items.getSkuId();
        //??????spu?????????
        R spuInfo = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoData = spuInfo.getData("data", new TypeReference<SpuInfoVo>() {
        });
        orderItemEntity.setSpuId(spuInfoData.getId());
        orderItemEntity.setSpuName(spuInfoData.getSpuName());
        orderItemEntity.setSpuBrand(spuInfoData.getBrandName());
        orderItemEntity.setCategoryId(spuInfoData.getCatalogId());

        //2????????????sku??????
        orderItemEntity.setSkuId(skuId);
        orderItemEntity.setSkuName(items.getTitle());
        orderItemEntity.setSkuPic(items.getImage());
        orderItemEntity.setSkuPrice(items.getPrice());
        orderItemEntity.setSkuQuantity(items.getCount());

        //??????StringUtils.collectionToDelimitedString???list???????????????String
        String skuAttrValues = StringUtils.collectionToDelimitedString(items.getSkuAttrValues(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttrValues);

        //3????????????????????????

        //4????????????????????????
        orderItemEntity.setGiftGrowth(items.getPrice().multiply(new BigDecimal(items.getCount())).intValue());
        orderItemEntity.setGiftIntegration(items.getPrice().multiply(new BigDecimal(items.getCount())).intValue());

        //5???????????????????????????
        orderItemEntity.setPromotionAmount(BigDecimal.ZERO);
        orderItemEntity.setCouponAmount(BigDecimal.ZERO);
        orderItemEntity.setIntegrationAmount(BigDecimal.ZERO);

        //??????????????????????????????.?????? - ??????????????????
        //???????????????
        BigDecimal origin = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        //??????????????????????????????????????????
        BigDecimal subtract = origin.subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(subtract);

        return orderItemEntity;
    }

    /**
     * ?????????????????????
     *
     * @param orderEntity
     * @param orderItemEntities
     */
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {

        //??????
        BigDecimal total = new BigDecimal("0.0");
        //?????????
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal intergration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");

        //??????????????????
        Integer integrationTotal = 0;
        Integer growthTotal = 0;

        //??????????????????????????????????????????????????????
        for (OrderItemEntity orderItem : orderItemEntities) {
            //??????????????????
            coupon = coupon.add(orderItem.getCouponAmount());
            promotion = promotion.add(orderItem.getPromotionAmount());
            intergration = intergration.add(orderItem.getIntegrationAmount());

            //??????
            total = total.add(orderItem.getRealAmount());

            //??????????????????????????????
            integrationTotal += orderItem.getGiftIntegration();
            growthTotal += orderItem.getGiftGrowth();

        }
        //1????????????????????????
        orderEntity.setTotalAmount(total);
        //??????????????????(??????+??????)
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setCouponAmount(coupon);
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(intergration);

        //???????????????????????????
        orderEntity.setIntegration(integrationTotal);
        orderEntity.setGrowth(growthTotal);

        //??????????????????(0-????????????1-?????????)
        orderEntity.setDeleteStatus(0);
    }
}