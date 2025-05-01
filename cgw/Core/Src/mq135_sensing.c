
MQ135_HandleTypeDef hmq_in;
MQ135_HandleTypeDef hmq_out;

typedef struct
{
    double benzene_ppm_in;
    double co_ppm_in;
    double co2_ppm_in;
    double smoke_ppm_in;

    double benzene_ppm_out;
    double co_ppm_out;
    double co2_ppm_out;
    double smoke_ppm_out;
} mq135_data;

mq135_data mq135;

void StartAqReaderTask(void *argument)
{
    mq135_data *mq135 = (mq135_data *)argument;
    for (;;)
    {
        mq135->benzene_ppm_in = MQ135_GetPPM(&hmq_in) / 10.000;
        mq135->co_ppm_in      = MQ135_GetCO_PPM(&hmq_in) / 9.00;
        mq135->co2_ppm_in     = MQ135_GetCO2_PPM(&hmq_in) / 5.00;
        mq135->smoke_ppm_in   = MQ135_GetSmoke_PPM(&hmq_in) / 50.00;

        mq135->benzene_ppm_out = MQ135_GetPPM(&hmq_out) / 10.000;
        mq135->co_ppm_out      = MQ135_GetCO_PPM(&hmq_out) / 9.00;
        mq135->co2_ppm_out     = MQ135_GetCO2_PPM(&hmq_out) / 5.00;
        mq135->smoke_ppm_out   = MQ135_GetSmoke_PPM(&hmq_out) / 50.00;

        // TeraTerm으로 uart통신해서 출력하기
        printf("\r\n=== Indoor Sensor ===\r\n");
        printf("Benzene : %.3f ppm \r\n", mq135->benzene_ppm_in);
        printf("CO : %.2f ppm\r\n", mq135->co_ppm_in);
        printf("CO2 : %.2f ppm\r\n", mq135->co2_ppm_in);
        printf("Smoke : %.2f ppm\r\n", mq135->smoke_ppm_in);

        printf("\r\n=== Outdoor Sensor ===\r\n");
        printf("Benzene : %.3f ppm \r\n", mq135->benzene_ppm_out);
        printf("CO : %.2f ppm\r\n", mq135->co_ppm_out);
        printf("CO2 : %.2f ppm\r\n", mq135->co2_ppm_out);
        printf("Smoke : %.2f ppm\r\n", mq135->smoke_ppm_out);

        osDelay(5000);
    }
}