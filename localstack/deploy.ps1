# deploy.ps1
param(
    [string]$FunctionName = "lambda-session",
    [string]$Handler = "com.colombia.eps.session.AwsLambdaHandler",
    [string]$Runtime = "java21",
    [string]$TableName = "session",
    [string]$SessionFunction = "sessionFunction",
    [string]$MainClass = "com.colombia.eps.session.SessionApplication",
    [string]$UrlFront = "http://localhost:4200",
    [string]$SpringProfile = "local"
)

# Configurar variables de entorno para LocalStack
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"
$env:AWS_ENDPOINT_URL = "http://localhost:4566"

Write-Host "Desplegando Lambda directamente en LocalStack..." -ForegroundColor Green
localstack start -d -e LAMBDA_DOCKER_NETWORK=bridge -e HOSTNAME_EXTERNAL=host.docker.internal -e DOCKER_HOST="unix:///var/run/docker.sock" -e LAMBDA_REMOTE_DOCKER=false -e LAMBDA_DOCKER_FLAGS='--network=bridge'

# Verificar que LocalStack esta corriendo
Write-Host "Verificando conexion con LocalStack..." -ForegroundColor Yellow
try {
    $output = & aws --endpoint-url=http://localhost:4566 sts get-caller-identity --output json 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "LocalStack conectado correctamente" -ForegroundColor Green
    } else {
        throw "LocalStack no disponible"
    }
} catch {
    Write-Host "Error: LocalStack no esta disponible. Asegurate de que este corriendo." -ForegroundColor Red
    Write-Host "Ejecuta: docker-compose up -d" -ForegroundColor Yellow
    exit 1
}

$currentDir = Split-Path -Leaf (Get-Location)
Write-Host "Directorio actual: $currentDir" -ForegroundColor Cyan

# Determinar la ruta relativa según desde dónde se ejecuta
if ($currentDir -eq "localstack") {
    # Se ejecuta desde la carpeta localstack
    $zipPath = "../build/function.zip"
    Write-Host "Ejecutándose desde carpeta localstack" -ForegroundColor Green
} else {
    # Se ejecuta desde la carpeta principal del proyecto
    $zipPath = "./build/function.zip"
    Write-Host "Ejecutándose desde carpeta principal del proyecto" -ForegroundColor Green
}


# Verificar que el archivo existe
if (-not (Test-Path $zipPat)) {
    Write-Host "Error: No se encontró el archivo $zipPath" -ForegroundColor Red
    Write-Host "Ubicación actual: $(Get-Location)" -ForegroundColor Yellow
    Write-Host "Archivos en el directorio actual:" -ForegroundColor Yellow
    Get-ChildItem -Name
    Write-Host "¿Existe la carpeta build/libs?" -ForegroundColor Yellow
    if (Test-Path "./build/libs") {
        Write-Host "Sí existe build/libs. Contenido:" -ForegroundColor Yellow
        Get-ChildItem "./build/libs" -Name
    } else {
        Write-Host "No existe la carpeta build/libs desde aquí" -ForegroundColor Red
    }
    exit 1
} else {
    Write-Host "JAR encontrado correctamente en: $zipPath" -ForegroundColor Green
}


# Funcion para ejecutar comando AWS y manejar errores
function Invoke-AwsCommand {
    param(
        [string]$Command,
        [string]$SuccessMessage,
        [string]$WarningMessage = "Recurso ya existe o error menor"
    )
    
    try {
        Invoke-Expression $Command
        if ($LASTEXITCODE -eq 0) {
            Write-Host $SuccessMessage -ForegroundColor Green
        } else {
            Write-Host $WarningMessage -ForegroundColor Yellow
        }
    } catch {
        Write-Host $WarningMessage -ForegroundColor Yellow
    }
}


# Crear tabla DynamoDB
Write-Host "Creando tabla DynamoDB..." -ForegroundColor Yellow
$dynamoCommand = "aws dynamodb create-table --endpoint-url=http://localhost:4566 --table-name $TableName --attribute-definitions AttributeName=id,AttributeType=S AttributeName=connection_time,AttributeType=S --key-schema AttributeName=id,KeyType=HASH AttributeName=connection_time,KeyType=RANGE --billing-mode PAY_PER_REQUEST"
Invoke-AwsCommand -Command $dynamoCommand -SuccessMessage "Tabla DynamoDB creada" -WarningMessage "Tabla DynamoDB ya existe"


# Crear rol IAM para Lambda 
Write-Host "Creando rol IAM..." -ForegroundColor Yellow

# Crear la política como objeto PowerShell y convertir a JSON
$assumeRolePolicy = @{
    Version = "2012-10-17"
    Statement = @(
        @{
            Effect = "Allow"
            Principal = @{
                Service = "lambda.amazonaws.com"
            }
            Action = "sts:AssumeRole"
        }
    )
} | ConvertTo-Json -Depth 10 -Compress

# Crear archivo temporal sin BOM
$policyFile = "temp-assume-role-policy.json"
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($policyFile, $assumeRolePolicy, $utf8NoBom)

$iamRoleCommand = "aws iam create-role --endpoint-url=http://localhost:4566 --role-name lambda-execution-role --assume-role-policy-document file://$policyFile"
Invoke-AwsCommand -Command $iamRoleCommand -SuccessMessage "Rol IAM creado" -WarningMessage "Rol IAM ya existe"

# Crear politica de permisos para Lambda
Write-Host "Creando politica de permisos..." -ForegroundColor Yellow

$lambdaPolicy = @{
    Version = "2012-10-17"
    Statement = @(
        @{
            Effect = "Allow"
            Action = @(
                "logs:CreateLogGroup",
                "logs:CreateLogStream", 
                "logs:PutLogEvents"
            )
            Resource = "arn:aws:logs:*:*:*"
        },
        @{
            Effect = "Allow"
            Action = @(
                "dynamodb:GetItem",
                "dynamodb:PutItem",
                "dynamodb:UpdateItem",
                "dynamodb:DeleteItem",
                "dynamodb:Query",
                "dynamodb:Scan",
                "dynamodb:DescribeTable"
            )
            Resource = "arn:aws:dynamodb:*:*:table/*"
        }
    )
} | ConvertTo-Json -Depth 10 -Compress

$lambdaPolicyFile = "temp-lambda-policy.json"
[System.IO.File]::WriteAllText($lambdaPolicyFile, $lambdaPolicy, $utf8NoBom)

$createPolicyCommand = "aws iam create-policy --endpoint-url=http://localhost:4566 --policy-name lambda-dynamodb-policy --policy-document file://$lambdaPolicyFile"
Invoke-AwsCommand -Command $createPolicyCommand -SuccessMessage "Politica de permisos creada" -WarningMessage "Politica ya existe"

# Adjuntar politica personalizada al rol (usando el ARN correcto de LocalStack)
Write-Host "Adjuntando politica personalizada al rol..." -ForegroundColor Yellow
$attachCustomPolicyCommand = "aws iam attach-role-policy --endpoint-url=http://localhost:4566 --role-name lambda-execution-role --policy-arn arn:aws:iam::000000000000:policy/lambda-dynamodb-policy"
Invoke-AwsCommand -Command $attachCustomPolicyCommand -SuccessMessage "Politica personalizada adjuntada" -WarningMessage "Politica ya adjunta"

# Intentar adjuntar politica basica de AWS Lambda (puede no existir en LocalStack)
Write-Host "Intentando adjuntar politica basica de AWS Lambda..." -ForegroundColor Yellow
$attachBasicPolicyCommand = "aws iam attach-role-policy --endpoint-url=http://localhost:4566 --role-name lambda-execution-role --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
Invoke-AwsCommand -Command $attachBasicPolicyCommand -SuccessMessage "Politica basica AWS adjuntada" -WarningMessage "Politica basica AWS no disponible en LocalStack (normal)"

# Limpiar archivos temporales
Remove-Item $policyFile -ErrorAction SilentlyContinue
Remove-Item $lambdaPolicyFile -ErrorAction SilentlyContinue

# Esperar un momento para propagacion
Write-Host "Esperando propagacion de roles..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

# Eliminar funcion existente si existe
Write-Host "Eliminando funcion Lambda existente (si existe)..." -ForegroundColor Yellow
try {
    aws lambda delete-function --endpoint-url=http://localhost:4566 --function-name $FunctionName 2>$null
    Write-Host "Funcion anterior eliminada" -ForegroundColor Green
} catch {
    Write-Host "Funcion no existia previamente" -ForegroundColor Yellow
}

# Crear funcion Lambda
Write-Host "Creando funcion Lambda..." -ForegroundColor Yellow
$createFunctionCommand = "aws lambda create-function --endpoint-url=http://localhost:4566 --function-name $FunctionName --runtime $Runtime --role arn:aws:iam::000000000000:role/lambda-execution-role --handler $Handler --zip-file fileb://$zipPat --environment Variables='{DYNAMO_TABLE_NAME=$TableName,AWS_ENDPOINT_URL=http://localhost:4566,SPRING_CLOUD_FUNCTION_DEFINITION=$SessionFunction,MAIN_CLASS=$MainClass,URL_FRONT=$UrlFront,SPRING_PROFILE=$SpringProfile}'"

Invoke-AwsCommand -Command $createFunctionCommand -SuccessMessage "Funcion Lambda creada exitosamente" -WarningMessage "Error creando funcion Lambda"

# Crear URL publica para la funcion Lambda
Write-Host "Creando URL publica para la funcion..." -ForegroundColor Yellow
aws lambda create-function-url-config `
    --endpoint-url=http://localhost:4566 `
    --function-name $FunctionName `
    --auth-type NONE

# Obtener la URL de la funcion
Write-Host "Obteniendo URL de la funcion..." -ForegroundColor Yellow
$urlInfo = aws lambda get-function-url-config `
    --endpoint-url=http://localhost:4566 `
    --function-name $FunctionName `
    --output json | ConvertFrom-Json

$functionUrl = $urlInfo.FunctionUrl
Write-Host "URL de tu Lambda: $functionUrl" -ForegroundColor Green
Write-Host "Despliegue completado!" -ForegroundColor Green
Write-Host "Funcion Lambda: $FunctionName" -ForegroundColor Cyan
Write-Host "URL de la funcion: $functionUrl" -ForegroundColor Cyan
Write-Host "Tabla DynamoDB: $TableName" -ForegroundColor Cyan
Write-Host "Endpoint LocalStack: http://localhost:4566" -ForegroundColor Cyan

# Mostrar informacion de la funcion creada
Write-Host "`nObteniendo informacion de la funcion..." -ForegroundColor Yellow
$getFunctionCommand = "aws lambda get-function --endpoint-url=http://localhost:4566 --function-name $FunctionName"
Invoke-AwsCommand -Command $getFunctionCommand -SuccessMessage "Informacion de la funcion obtenida" -WarningMessage "No se pudo obtener informacion de la funcion"