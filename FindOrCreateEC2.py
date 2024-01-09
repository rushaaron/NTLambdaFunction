import boto3

def lambda_handler(event, context):
    # Define the region and EC2 client
    region = 'us-east-1'
    ec2_client = boto3.client('ec2', region_name=region)

    ec2_instance_id = searchForRunningInstance(ec2_client)

    #If ec2 is not existing, have to create a new one
    if ec2_instance_id is None:
        ec2_instance_id = createInstance(ec2_client)
    
    #If failed to create new EC2
    if ec2_instance_id is None: 
        return {
            'statusCode': 500,
            'body': "Failed to find and create instance"
        }
    else :
        return {
            'statusCode': 200,
            'body': ec2_instance_id
        }

def searchForRunningInstance(ec2_client):
        try:
            # Get instances with specific filters (running or pending and with tag 'Name' as 'TestFromLambda')
            filters = [
                {'Name': 'instance-state-name', 'Values': ['running', 'pending']},
                {'Name': 'tag:Name', 'Values': ['TestFromLambda']}
            ]

            response = ec2_client.describe_instances(Filters=filters)

            instances = []
            for reservation in response['Reservations']:
                for instance in reservation['Instances']:
                    instances.append(instance)
            if instances:
                return instances[0]['InstanceId']
            else:
                print('No instances found')
                return None
        
        except Exception as e:
            print(f"Failed when fetching instances because {str(e)}")
            return None

    
def createInstance(ec2_client):
     # Define parameters for the instance
    instance_params = {
        'ImageId': 'ami-0812cdeb2e949b922',  # Replace 'ami-xxxxxxxx' with your desired AMI ID
        'InstanceType': 't2.micro',
        'KeyName': 'tracerEC2',  # Replace 'your-key-pair' with your key pair name
        'MinCount': 1,
        'MaxCount': 1,
                'TagSpecifications': [
            {
                'ResourceType': 'instance',
                'Tags': [
                    {
                        'Key': 'Name',
                        'Value': 'TestFromLambda'  # Set your desired instance name here
                    }
                ]
            }
        ]
    }
    
    # Create the EC2 instance
    try:
        print('Creating new instance')
        response = ec2_client.run_instances(**instance_params)
        return response['Instances'][0]['InstanceId']
    except Exception as e:
        return None
    
