# AWS Deployment

Transport is deployed as an ECS Fargate service in the shared AWS account.

## Current Resources

| Resource | Value |
|---|---|
| Region | `eu-west-1` |
| ECS cluster | `junior-workshop-2026-vlc` |
| ECS service | `transport-service` |
| Task family | `transport-service` |
| ECR image | `822414985516.dkr.ecr.eu-west-1.amazonaws.com/transport-service:latest` |
| Public URL | `http://taller-deploy-aws-2026-nlb-pulz-adee7212e878911f.elb.eu-west-1.amazonaws.com:8080` |
| RDS identifier | `transport-postgres-dev` |
| Database | `transport` |
| Log group | `/ecs/transport-service` |

## Runtime Configuration

Runtime values are injected through AWS Secrets Manager:

```text
transport-service/db-url
transport-service/db-username
transport-service/db-password
transport-service/rabbitmq-host
transport-service/rabbitmq-port
transport-service/rabbitmq-username
transport-service/rabbitmq-password
transport-service/rabbitmq-vhost
transport-service/rabbitmq-ssl-enabled
```

If a secret changes, force a new ECS deployment so the task reads the latest value:

```bash
aws ecs update-service \
  --cluster junior-workshop-2026-vlc \
  --service transport-service \
  --force-new-deployment
```

## Deploying Code Changes

Run these commands from the repository root after changing Java/config/Dockerfile code:

```bash
aws ecr get-login-password --region eu-west-1 \
  | docker login --username AWS --password-stdin 822414985516.dkr.ecr.eu-west-1.amazonaws.com

docker build -t transport-service .
docker tag transport-service:latest 822414985516.dkr.ecr.eu-west-1.amazonaws.com/transport-service:latest
docker push 822414985516.dkr.ecr.eu-west-1.amazonaws.com/transport-service:latest

aws ecs update-service \
  --cluster junior-workshop-2026-vlc \
  --service transport-service \
  --force-new-deployment
```

Docs/test-only changes do not require an AWS redeploy.

## Verifying The Deployment

Basic REST check:

```bash
curl -i http://taller-deploy-aws-2026-nlb-pulz-adee7212e878911f.elb.eu-west-1.amazonaws.com:8080/trucks
```

Full smoke test against AWS and CloudAMQP:

```powershell
.\verify-e2e.ps1 `
  -Password "your_cloudamqp_password" `
  -ServiceUrl "http://taller-deploy-aws-2026-nlb-pulz-adee7212e878911f.elb.eu-west-1.amazonaws.com:8080"
```

## GitHub Actions Smoke Pipeline

The repository includes a manual GitHub Actions workflow:

```text
.github/workflows/aws-smoke.yml
```

Run it from GitHub:

```text
Actions -> aws-smoke -> Run workflow
```

Required GitHub secret:

```text
CLOUDAMQP_PASSWORD
```

Optional GitHub repository variables:

```text
CLOUDAMQP_HOST=seal.lmq.cloudamqp.com
CLOUDAMQP_VHOST=ibvztclz
CLOUDAMQP_USER=ibvztclz
```

This workflow is intentionally manual. It validates the deployed AWS service and the real CloudAMQP flow, so it should not block normal PR builds when external infrastructure is unavailable.

ECS service status:

```bash
aws ecs describe-services \
  --cluster junior-workshop-2026-vlc \
  --services transport-service \
  --query 'services[0].[desiredCount,runningCount,pendingCount,events[0].message]' \
  --output table
```

Target group health:

```bash
aws elbv2 describe-target-health \
  --target-group-arn arn:aws:elasticloadbalancing:eu-west-1:822414985516:targetgroup/taller-deploy-aws-2026-pulz-ip/c9ab57b03f7fc910 \
  --query 'TargetHealthDescriptions[*].[Target.Id,Target.Port,TargetHealth.State,TargetHealth.Reason]' \
  --output table
```

## Important Security Note

If a CloudAMQP password is exposed, rotate it in CloudAMQP, then update AWS Secrets Manager:

```bash
aws secretsmanager put-secret-value \
  --secret-id transport-service/rabbitmq-password \
  --secret-string "new_real_password"

aws ecs update-service \
  --cluster junior-workshop-2026-vlc \
  --service transport-service \
  --force-new-deployment
```
